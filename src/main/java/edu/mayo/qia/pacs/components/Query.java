package edu.mayo.qia.pacs.components;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DimseRSP;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import edu.mayo.qia.pacs.Notion;
import edu.mayo.qia.pacs.ctp.Anonymizer;
import edu.mayo.qia.pacs.dicom.DcmMoveException;
import edu.mayo.qia.pacs.dicom.DcmQR;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {
  static Logger logger = Logger.getLogger(Query.class);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int queryKey;

  public String status;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMM dd, yyyy HH:mm")
  public Date createdTimestamp = new Date();
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMM dd, yyyy HH:mm")
  public Date lastQueryTimestamp = new Date();

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
  @JoinColumn(name = "PoolKey")
  public Pool pool;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
  @JoinColumn(name = "DestinationPoolKey")
  public Pool destinationPool;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
  @JoinColumn(name = "DeviceKey")
  public Device device;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "query", fetch = FetchType.EAGER)
  public Set<Item> items = new HashSet<Item>();

  @JsonIgnore
  @Transient
  Future<String> queryFuture;
  @JsonIgnore
  @Transient
  Future<String> fetchFuture;

  /**
   * Construct a query object and return it
   * 
   * @throws Exception
   */
  public static Query constructQuery(String filename, InputStream is) throws Exception {
    Query query = new Query();
    // Handle XLS for the moment
    Workbook workbook = null;
    if (filename.toLowerCase().endsWith(".xlsx")) {
      workbook = new XSSFWorkbook(is);
    }
    if (filename.toLowerCase().endsWith(".xls")) {
      workbook = new HSSFWorkbook(is);
    }

    Map<String, Integer> headerMap = new HashMap<String, Integer>();
    Notion.checkAssertion(workbook.getNumberOfSheets() > 0, "Could not find a sheet in the workbook");
    Sheet sheet = workbook.getSheetAt(0);
    Notion.checkAssertion(sheet.getPhysicalNumberOfRows() > 0, "Expecting a header row");
    Iterator<Cell> headerIterator = sheet.getRow(0).cellIterator();
    while (headerIterator.hasNext()) {
      Cell headerCell = headerIterator.next();
      headerMap.put(headerCell.getStringCellValue(), headerCell.getColumnIndex());
    }
    Notion.checkAssertion(headerMap.containsKey("PatientID"), "Could not find PatientID column");

    query.status = "Created";

    Iterator<Row> rowIterator = sheet.iterator();
    // Skip the header row
    rowIterator.next();
    while (rowIterator.hasNext()) {

      Row row = rowIterator.next();
      Item item = new Item();

      Notion.checkAssertion(row.getCell(headerMap.get("PatientID")) != null, "Row " + row.getRowNum() + " does not contain a PatientID");
      item.status = "created";
      item.patientName = getColumn(headerMap, row, "PatientName");
      item.patientID = getColumn(headerMap, row, "PatientID");
      item.accessionNumber = getColumn(headerMap, row, "AccessionNumber");
      item.patientBirthDate = getColumn(headerMap, row, "PatientBirthDate");
      item.studyDate = getColumn(headerMap, row, "StudyDate");
      item.modalitiesInStudy = getColumn(headerMap, row, "ModalitiesInStudy");
      item.studyDescription = getColumn(headerMap, row, "StudyDescription");
      item.anonymizedID = getColumn(headerMap, row, "AnonymizedID");
      item.anonymizedName = getColumn(headerMap, row, "AnonymizedName");
      if (item.patientID != null) {
        item.query = query;
        query.items.add(item);
      }
    }
    return query;
  }

  static String getAttribute(JsonNode node, String key) {
    return node.has(key) ? node.get(key).asText() : null;
  }

  public static Query constructQuery(JsonNode json) {
    Query query = new Query();
    Iterator<JsonNode> iterator = json.get("items").elements();
    while (iterator.hasNext()) {
      JsonNode node = iterator.next();
      Item item = new Item();
      item.patientID = getAttribute(node, "PatientID");
      item.patientName = getAttribute(node, "PatientName");
      item.anonymizedName = getAttribute(node, "AnonymizedName");
      item.anonymizedID = getAttribute(node, "AnonymizedID");
      if (item.patientID != null) {
        query.items.add(item);
        item.query = query;
      }
    }
    return query;
  }

  static String getColumn(Map<String, Integer> headerMap, Row row, String column) {
    if (!headerMap.containsKey(column)) {
      return null;
    }
    Cell cell = row.getCell(headerMap.get(column));
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      return Double.toString(cell.getNumericCellValue());
    }
    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
      return cell.getStringCellValue();
    } else {
      return null;
    }
  }

  // Implement a C-FIND and store results away...
  public void executeQuery() {
    if (queryFuture != null && !queryFuture.isDone()) {
      return;
    }
    ExecutorService executor = Notion.context.getBean("executor", ExecutorService.class);
    queryFuture = executor.submit(new Callable<String>() {
      public String call() {
        Thread.currentThread().setName("Query " + device);
        JdbcTemplate template = Notion.context.getBean(JdbcTemplate.class);
        template.update("update QUERY set Status = ? where QueryKey = ?", "query pending", queryKey);
        template.update("update QUERYITEM set Status = ? where QueryKey = ?", "query pending", queryKey);
        for (Item item : items) {
          template.update("delete from QUERYRESULT where QueryItemKey = ?", item.queryItemKey);
        }
        for (final Item item : items) {
          template.update("update QUERYITEM set Status = ? where QueryItemKey = ?", "working", item.queryItemKey);

          DcmQR dcmQR = new DcmQR(destinationPool.applicationEntityTitle);
          dcmQR.setRemoteHost(device.hostName);
          dcmQR.setRemotePort(device.port);
          dcmQR.setCalledAET(device.applicationEntityTitle);
          dcmQR.setCalling(destinationPool.applicationEntityTitle);

          // Add our query parameters
          Map<String, String> map = item.getTagMap();
          for (String key : map.keySet()) {
            dcmQR.addMatchingKey(Tag.toTagPath(key), map.get(key));
          }
          try {
            dcmQR.open();
            DimseRSP response = dcmQR.queryAll();
            while (response.next()) {
              DicomObject command = response.getCommand();
              if (CommandUtils.isPending(command)) {
                DicomObject ds = response.getDataset();
                String status = ds.contains(Tag.StudyInstanceUID) ? "success" : "fail";
                //@formatter:off
                template.update("insert into QUERYRESULT ( "
                    + "QueryItemKey,"
                    + "Status,"
                    + "DoFetch,"
                    + "StudyInstanceUID,"
                    + "PatientName,"
                    + "PatientID,"
                    + "AccessionNumber,"
                    + "PatientBirthDate,"
                    + "StudyDate,"
                    + "ModalitiesInStudy,"
                    + "StudyDescription"
                    + " ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )",
                    item.queryItemKey,
                    status,
                    "F",
                    ds.getString(Tag.StudyInstanceUID ),
                    ds.getString(Tag.PatientName, (String)null),
                    ds.getString(Tag.PatientID, (String)null),
                    ds.getString(Tag.AccessionNumber, (String)null),
                    ds.getString(Tag.PatientBirthDate, (String)null),
                    ds.getString(Tag.StudyDate, (String)null),
                    ds.getString(Tag.ModalitiesInStudy, (String)null),
                    ds.getString(Tag.StudyDescription, (String)null)
                    );
                //@formatter:on
              }
            }
            template.update("update QUERYITEM set Status = ? where QueryItemKey = ?", "query complete", item.queryItemKey);
          } catch (Exception e) {
            logger.error("Caught error in query", e);
            template.update("update QUERYITEM set Status = ? where QueryItemKey = ?", "query failed", item.queryItemKey);
          } finally {
            try {
              dcmQR.close();
            } catch (Exception e) {
              logger.error("Error closing query connection", e);
            }
          }
        }
        template.update("update QUERY set Status = ? where QueryKey = ?", "query completed", queryKey);
        Thread.currentThread().setName("Idle");
        return "completed";
      }
    });
  }

  /** Update the query, by triggering a fetch on each query result */
  public void update(Query update) {
    if (update.queryKey == this.queryKey) {
      JdbcTemplate template = Notion.context.getBean(JdbcTemplate.class);
      for (Item updateItem : update.items) {
        // Find this item...
        for (Item item : items) {
          if (item.queryItemKey == updateItem.queryItemKey) {
            for (Result updateResult : updateItem.items) {
              for (Result result : item.items) {
                if (result.queryResultKey == updateResult.queryResultKey) {
                  logger.info(updateResult.doFetch);
                  String doFetch = updateResult.doFetch ? "T" : "F";
                  template.update("update QUERYRESULT set DoFetch = ? where QueryResultKey = ?", doFetch, result.queryResultKey);
                  continue;
                }
              }
            }
            continue;
          }
        }
      }
    }
  }

  public void doFetch() {
    logger.debug("Queuing fetch");

    if (fetchFuture != null && !fetchFuture.isDone()) {
      return;
    }

    final JdbcTemplate template = Notion.context.getBean(JdbcTemplate.class);
    template.update("update QUERY set Status = ? where QueryKey = ?", "fetch pending", queryKey);

    fetchFuture = Notion.context.getBean("executor", ExecutorService.class).submit(new Callable<String>() {
      public String call() {
        final JdbcTemplate template = Notion.context.getBean(JdbcTemplate.class);
        template.update("update QUERY set Status = ? where QueryKey = ?", "fetch pending", queryKey);
        template.update("update QUERYRESULT set Status = ? where QueryItemKey in ( select QueryItemKey from QUERYITEM where QueryKey = ?) ", "fetch pending", queryKey);
        template.update("update QUERYRESULT set Status = ? where QueryItemKey in ( select QueryItemKey from QUERYITEM where QueryKey = ?) and DoFetch = 'F'", "", queryKey);
        Thread.currentThread().setName("Fetch " + device);
        PoolManager poolManager = Notion.context.getBean(PoolManager.class);
        PoolContainer poolContainer = poolManager.getContainer(pool.poolKey);
        Anonymizer anonymizer = Notion.context.getBean("anonymizer", Anonymizer.class);
        anonymizer.setPool(poolContainer.getPool());

        for (final Item item : items) {
          for (final Result result : item.items) {
            if (!result.doFetch) {
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "", result.queryResultKey);
              continue;
            }
            template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "fetching", result.queryResultKey);
            DcmQR dcmQR = new DcmQR(destinationPool.applicationEntityTitle);
            dcmQR.setRemoteHost(device.hostName);
            dcmQR.setRemotePort(device.port);
            dcmQR.setCalledAET(device.applicationEntityTitle);
            dcmQR.setCalling(destinationPool.applicationEntityTitle);
            dcmQR.setMoveDest(destinationPool.applicationEntityTitle);
            try {
              // Create the entries
              anonymizer.setValue("PatientName", result.patientName, item.anonymizedName);
              anonymizer.setValue("PatientID", result.patientID, item.anonymizedID);
              dcmQR.qrStudy(result.studyInstanceUID);
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "moving", result.queryResultKey);

              // Initiate the move
              if (pool.poolKey != destinationPool.poolKey) {
                // Move and delete, otherwise ignore
                if (!poolManager.getContainer(destinationPool.poolKey).moveStudyTo(result.studyInstanceUID, poolContainer)) {
                  template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "fail: could not move study ", result.queryResultKey);
                } else {
                  // Delete the study
                  poolManager.getContainer(destinationPool.poolKey).deleteStudy(result.studyInstanceUID);
                }
              }
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "completed", result.queryResultKey);

            } catch (DcmMoveException e) {
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "fail: " + e.toString(), result.queryResultKey);
            } catch (Exception e) {
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "fail: unknown exception " + e.toString(), result.queryResultKey);
            }
          }
        }
        template.update("update QUERY set Status = ? where QueryKey = ?", "fetch completed", queryKey);
        logger.debug("Fetch Compeleted");
        Thread.currentThread().setName("Idle");
        return "complete";
      }
    });
  }

  /** Form the CVS for this query. */
  @Transient
  @JsonIgnore
  public Workbook generateSpreadSheet() {
    final JdbcTemplate template = Notion.context.getBean(JdbcTemplate.class);

    XSSFWorkbook workbook = new XSSFWorkbook();
    final XSSFSheet sheet = workbook.createSheet("query");
    XSSFRow header = sheet.createRow(0);
    header.createCell(0, Cell.CELL_TYPE_STRING).setCellValue("PatientName");
    header.createCell(1, Cell.CELL_TYPE_STRING).setCellValue("PatientID");
    header.createCell(2, Cell.CELL_TYPE_STRING).setCellValue("AccessionNumber");
    header.createCell(3, Cell.CELL_TYPE_STRING).setCellValue("PatientBirthDate");
    header.createCell(4, Cell.CELL_TYPE_STRING).setCellValue("StudyDate");
    header.createCell(5, Cell.CELL_TYPE_STRING).setCellValue("ModalitiesInStudy");
    header.createCell(6, Cell.CELL_TYPE_STRING).setCellValue("StudyDescription");
    header.createCell(7, Cell.CELL_TYPE_STRING).setCellValue("AnonymizedID");
    header.createCell(8, Cell.CELL_TYPE_STRING).setCellValue("AnonymizedName");
    template.query("select * from QUERYITEM where QueryKey = ?", new Object[] { queryKey }, new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        XSSFRow row = sheet.createRow(1);
        row.createCell(0, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("PatientName"));
        row.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("PatientID"));
        row.createCell(2, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("AccessionNumber"));
        row.createCell(3, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("PatientBirthDate"));
        row.createCell(4, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("StudyDate"));
        row.createCell(5, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("ModalitiesInStudy"));
        row.createCell(6, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("StudyDescription"));
        row.createCell(7, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("AnonymizedID"));
        row.createCell(8, Cell.CELL_TYPE_STRING).setCellValue(rs.getString("AnonymizedName"));

      }
    });
    return workbook;

  }
}
