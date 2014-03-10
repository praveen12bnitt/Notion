package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dcm4che2.net.ConfigurationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.components.Query;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryTest extends PACSTest {

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  InputStream getResource(String fileName) throws Exception {
    Resource resource = PACS.context.getResource("classpath:" + fileName);
    return resource.getInputStream();
  }

  @Test
  public void xls() throws Exception {

    InputStream is = getResource("Query/QueryTemplate.xls");

    // Get the workbook instance for XLS file
    Workbook workbook = new HSSFWorkbook(is);

    assertTrue("Expected more than 0 worksheets", workbook.getNumberOfSheets() > 0);

    // Get first sheet from the workbook
    Sheet sheet = workbook.getSheetAt(0);

    assertEquals(3, sheet.getPhysicalNumberOfRows());
    assertEquals(9, sheet.getRow(0).getPhysicalNumberOfCells());

    assertEquals("PatientName", sheet.getRow(0).getCell(0).getStringCellValue());
    assertEquals("Hurt John", sheet.getRow(1).getCell(0).getStringCellValue());
    is.close();
  }

  @Test
  public void xlsx() throws Exception {

    InputStream is = getResource("Query/QueryTemplate.xlsx");

    // Get the workbook instance for XLS file
    Workbook workbook = new XSSFWorkbook(is);

    // Get first sheet from the workbook
    Sheet sheet = workbook.getSheetAt(0);

    assertEquals(3, sheet.getPhysicalNumberOfRows());
    assertEquals(9, sheet.getRow(0).getPhysicalNumberOfCells());

    assertEquals("PatientName", sheet.getRow(0).getCell(0).getStringCellValue());
    assertEquals("Hurt John", sheet.getRow(1).getCell(0).getStringCellValue());
    is.close();
  }

  @Test
  public void constructFromXLSX() throws Exception {
    Query query;
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, true);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    Session session = sessionFactory.openSession();
    InputStream is = getResource("Query/QueryTemplate.xlsx");
    try {
      session.beginTransaction();
      query = Query.constructQuery("QueryTemplate.xlsx", is);
      query.pool = pool;
      query.destinationPool = pool;
      query.device = device;
      session.save(query);
      assertEquals(2, query.items.size());
      session.getTransaction().commit();
    } finally {
      is.close();
      session.close();
    }
    assertEquals(2, (int) template.queryForObject("select count(*) from QUERYITEM where QueryKey = ?", Integer.class, query.queryKey));
  }

  @Test
  public void uploadQuery() throws Exception {
    Query query;

    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, true);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    InputStream is = getResource("Query/QueryTemplate.xlsx");
    String sContentDisposition = "attachment; filename=\"QueryTemplate.xlsx\"";
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").path(Integer.toString(pool.poolKey)).path("query").build();

    WebResource fileResource = client.resource(uri);

    final FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", PACS.context.getResource("classpath:Query/QueryTemplate.xlsx").getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
    multiPart.field("destinationPoolKey", Integer.toString(pool.poolKey));
    multiPart.field("deviceKey", Integer.toString(device.deviceKey));

    final ClientResponse response = fileResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, multiPart);

    // ClientResponse response =
    // fileResource.type(MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition",
    // sContentDisposition).post(ClientResponse.class, is);
    // See what we got back
    query = response.getEntity(Query.class);
    assertEquals(pool.poolKey, query.pool.poolKey);
    assertEquals(2, (int) template.queryForObject("select count(*) from QUERYITEM where QueryKey = ?", Integer.class, query.queryKey));
  }

  @Test
  public void executeQuery() throws Exception {
    // My pool
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, true);
    pool = createPool(pool);

    // "PACS" pool
    uid = UUID.randomUUID();
    aet = uid.toString().substring(0, 10);
    Pool pacsPool = createPool(new Pool(aet, aet, aet, false));

    // "PACS" pool
    uid = UUID.randomUUID();
    aet = uid.toString().substring(0, 10);
    Pool destinationPool = createPool(new Pool(aet, aet, aet, false));

    // Flow of images is:
    // PACS -> destinationPool -> pool

    // Let the PACS pool know about the destination pool
    createDevice(new Device(destinationPool.applicationEntityTitle, "localhost", DICOMPort, pacsPool));

    // The device we will use to query the "PACS" pool
    Device queryDevice = createDevice(new Device(pacsPool.applicationEntityTitle, "localhost", DICOMPort, destinationPool.applicationEntityTitle, pool));

    // Send some test data to the PACS pool
    List<File> testSeries = sendDICOM(pacsPool.applicationEntityTitle, destinationPool.applicationEntityTitle, "TOF/IMAGE001.dcm");

    // Upload our query
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").path(Integer.toString(pool.poolKey)).path("query").build();
    WebResource fileResource = client.resource(uri);
    final FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", PACS.context.getResource("classpath:Query/QueryTemplate.xlsx").getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
    multiPart.field("destinationPoolKey", Integer.toString(destinationPool.poolKey));
    multiPart.field("deviceKey", Integer.toString(queryDevice.deviceKey));

    ClientResponse response = fileResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, multiPart);

    Query query = response.getEntity(Query.class);

    uri = UriBuilder.fromUri(baseUri).path("/pool").path(Integer.toString(pool.poolKey)).path("query").path(Integer.toString(query.queryKey)).build();
    logger.debug("Loading: " + uri);
    response = client.resource(uri).accept(JSON).get(ClientResponse.class);
    assertEquals("Got result", 200, response.getStatus());

  }
}
