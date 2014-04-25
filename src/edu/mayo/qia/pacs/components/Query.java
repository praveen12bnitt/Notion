package edu.mayo.qia.pacs.components;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DimseRSP;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.ctp.Anonymizer;
import edu.mayo.qia.pacs.dicom.DcmMoveException;
import edu.mayo.qia.pacs.dicom.DcmQR;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {
  static Logger logger = Logger.getLogger(Query.class);

  @Id
  @GeneratedValue
  public int queryKey;

  public String status;

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
    PACS.checkAssertion(workbook.getNumberOfSheets() > 0, "Could not find a sheet in the workbook");
    Sheet sheet = workbook.getSheetAt(0);
    PACS.checkAssertion(sheet.getPhysicalNumberOfRows() > 0, "Expecting a header row");
    Iterator<Cell> headerIterator = sheet.getRow(0).cellIterator();
    while (headerIterator.hasNext()) {
      Cell headerCell = headerIterator.next();
      headerMap.put(headerCell.getStringCellValue(), headerCell.getColumnIndex());
    }
    // PACS.checkAssertion(headerMap.containsKey("PatientName"),
    // "Could not find PatientName column");
    PACS.checkAssertion(headerMap.containsKey("PatientID"), "Could not find PatientID column");

    query.status = "Created";

    Iterator<Row> rowIterator = sheet.iterator();
    // Skip the header row
    rowIterator.next();
    while (rowIterator.hasNext()) {

      Row row = rowIterator.next();
      Item item = new Item();

      // PACS.checkAssertion(row.getCell(headerMap.get("PatientName")) != null,
      // "Row " + row.getRowNum() + " does not contain a PatientName");
      PACS.checkAssertion(row.getCell(headerMap.get("PatientID")) != null, "Row " + row.getRowNum() + " does not contain a PatientID");
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
    PACS.context.getBean("executor", Executor.class).execute(new Runnable() {




        public void run() {
          Thread.currentThread().setName ( "Query " + device );
          JdbcTemplate template = PACS.context.getBean(JdbcTemplate.class);
          template.update("update QUERY set Status = ? where QueryKey = ?", "Query Pending", queryKey);
          for (final Item item : items) {

          DcmQR dcmQR = new DcmQR();
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
            while ( response.next()) {
              DicomObject command = response.getCommand();
              if ( CommandUtils.isPending(command)) {
                DicomObject ds = response.getDataset();
                String status = ds.contains(Tag.StudyInstanceUID) ? "success" : "fail";
                // @formatter:off
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
                // @formatter:on
              }
            }
            dcmQR.close();
          } catch (Exception e) {
            logger.error("Caught error in query", e);
          }
          template.update("update QUERYITEM set Status = ? where QueryItemKey = ?", "complete", item.queryItemKey);
        }
        template.update("update QUERY set Status = ? where QueryKey = ?", "Query Completed", queryKey);
        Thread.currentThread().setName("Idle");
      }
    });
  }

  /** Update the query, by triggering a fetch on each query result */
  public void update(Query update) {
    if (update.queryKey == this.queryKey) {
      JdbcTemplate template = PACS.context.getBean(JdbcTemplate.class);
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
    PACS.context.getBean("executor", Executor.class).execute(new Runnable() {
      public void run() {
        final JdbcTemplate template = PACS.context.getBean(JdbcTemplate.class);
        template.update("update QUERY set Status = ? where QueryKey = ?", "Fetch Pending", queryKey);
        template.update("update QUERYRESULT set Status = ? where QueryItemKey in ( select QueryItemKey from QUERYITEM where QueryKey = ?) ", "pending fetch", queryKey);
        Thread.currentThread().setName("Fetch " + device);
        PoolManager poolManager = PACS.context.getBean(PoolManager.class);
        PoolContainer poolContainer = poolManager.getContainer(pool.poolKey);
        Anonymizer anonymizer = PACS.context.getBean("anonymizer", Anonymizer.class);
        anonymizer.setPool(poolContainer.getPool());

        for (final Item item : items) {
          for (final Result result : item.items) {
            if (!result.doFetch) {
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "not fetching", result.queryResultKey);
              continue;
            }
            template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "fetching", result.queryResultKey);
            DcmQR dcmQR = new DcmQR();
            dcmQR.setRemoteHost(device.hostName);
            dcmQR.setRemotePort(device.port);
            dcmQR.setCalledAET(device.applicationEntityTitle);
            dcmQR.setCalling(destinationPool.applicationEntityTitle);
            dcmQR.setMoveDest(destinationPool.applicationEntityTitle);
            try {
              dcmQR.qrStudy(result.studyInstanceUID);
              template.update("update QUERYRESULT set Status = ? where QueryResultKey = ?", "moving", result.queryResultKey);

              // Initiate the move
              if (pool.poolKey != destinationPool.poolKey) {
                // Move and delete, otherwise ignore
                // Create the entries
                anonymizer.setValue("PatientName", result.patientName, item.anonymizedName);
                anonymizer.setValue("PatientID", result.patientID, item.anonymizedID);
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
        template.update("update QUERY set Status = ? where QueryKey = ?", "Fetch Completed", queryKey);
        logger.debug("Fetch Compeleted");
        Thread.currentThread().setName("Idle");
      }
    });
  }
}
