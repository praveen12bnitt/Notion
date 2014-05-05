package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.representation.Form;

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.components.PoolContainer;
import edu.mayo.qia.pacs.components.PoolManager;
import edu.mayo.qia.pacs.dicom.DcmQR;

@Component
@RunWith(SpringJUnit4ClassRunner.class)
public class PoolTest extends PACSTest {
  static Logger logger = Logger.getLogger(PoolTest.class);

  @Autowired
  PoolManager poolManager;

  @Test
  public void listPools() {
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    logger.debug("Loading: " + uri);
    response = client.resource(uri).accept(JSON).get(ClientResponse.class);
    assertEquals("Got result", 200, response.getStatus());
  }

  @Test
  public void createPool() {
    // CURL Code
    /*
     * curl -X POST -H "Content-Type: application/json" -d
     * '{"name":"foo","path":"bar"}' http://localhost:11118/pool
     */
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    Pool pool = new Pool("empty", "empty", "empty", false);
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, pool);
    assertEquals("Got result", 200, response.getStatus());
    pool = response.getEntity(Pool.class);
    logger.info("Entity back: " + pool);
    assertTrue("Assigned an id", pool.poolKey != 0);

    // Pool should have 2 scripts attached
    uri = UriBuilder.fromUri(baseUri).path("/pool/").path("" + pool.poolKey).path("/script").build();
    response = client.resource(uri).accept(JSON).get(ClientResponse.class);
    assertEquals("Got result", 200, response.getStatus());
  }

  @Test
  public void invalidAETitle() {
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    for (String name : new String[] { "no spaces", "no !", "{", "#", "thisiswaytoolongofaname_you_think" }) {
      Pool pool = new Pool("garf", "empty", name, false);
      response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, pool);
      assertEquals("Got result", Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
  }

  @Test
  public void deletePool() throws Exception {
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, false);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    sendDICOM(aet, aet, "TOF/*.dcm");

    List<Integer> studyKeys = template.queryForList("select StudyKey from STUDY where STUDY.PoolKey = ? ", new Object[] { pool.poolKey }, Integer.class);
    assertEquals("Study", 1, studyKeys.size());
    List<Integer> seriesKeys = template.queryForList("select SERIES.SeriesKey from SERIES, STUDY where SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = ? ", new Object[] { pool.poolKey }, Integer.class);
    assertEquals("Series", 2, seriesKeys.size());
    List<Integer> instanceKeys = template.queryForList("select InstanceKey from INSTANCE, SERIES, STUDY where INSTANCE.SeriesKey = SERIES.SeriesKey and SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = ? ", new Object[] { pool.poolKey }, Integer.class);
    assertEquals("Instance", 164, instanceKeys.size());
    List<String> filePaths = template.queryForList("select FilePath from INSTANCE, SERIES, STUDY where INSTANCE.SeriesKey = SERIES.SeriesKey and SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = ? ", new Object[] { pool.poolKey }, String.class);
    assertEquals("Files", 164, filePaths.size());
    assertEquals("Device", new Integer(1), template.queryForObject("select count(*) from DEVICE where PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Script", new Integer(2), template.queryForObject("select count(*) from SCRIPT where PoolKey = " + pool.poolKey, Integer.class));

    // Create
    URI uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/lookup").path("create").build();
    Form form = new Form();
    form.add("Type", "PatientName");
    form.add("Name", "Mr. Magoo");
    form.add("Value", "Rikki-tikki-tavvi");
    ClientResponse response = client.resource(uri).post(ClientResponse.class, form);
    assertEquals("Status", 200, response.getStatus());
    assertEquals("Lookup", new Integer(1), template.queryForObject("select count(*) from LOOKUP where PoolKey = " + pool.poolKey, Integer.class));

    PoolContainer container = poolManager.getContainer(pool.poolKey);
    container.delete();

    assertEquals("Instance", new Integer(0), template.queryForObject("select count(*) from INSTANCE, SERIES, STUDY where INSTANCE.SeriesKey = SERIES.SeriesKey and SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Series", new Integer(0), template.queryForObject("select count(*) from SERIES, STUDY where SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Study", new Integer(0), template.queryForObject("select count(*) from STUDY where STUDY.PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Pool", new Integer(0), template.queryForObject("select count(*) from POOL where PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Device", new Integer(0), template.queryForObject("select count(*) from DEVICE where PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Lookup", new Integer(0), template.queryForObject("select count(*) from LOOKUP where PoolKey = " + pool.poolKey, Integer.class));
    assertEquals("Script", new Integer(0), template.queryForObject("select count(*) from SCRIPT where PoolKey = " + pool.poolKey, Integer.class));
    for (String filePath : filePaths) {
      File file = new File(container.getPoolDirectory(), filePath);
      assertFalse("File: " + file, file.exists());
    }
    assertNull("Manager", poolManager.getContainer(pool.poolKey));
  }

  @Test
  public void updateOnNewSeries() throws Exception {

    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, true);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    String accessionNumber = "AccessionNumber-1234";
    String script;
    script = "'" + accessionNumber + "'";
    template.update("insert into SCRIPT ( PoolKey,  Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "AccessionNumber", script);

    String patientName = "PN-1234";
    script = "'" + patientName + "'";
    template.update("insert into SCRIPT ( PoolKey,  Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "PatientName", script);

    String patientID = "MRA-0068-MRA-0068";
    script = "tags.PatientID + '-' + tags.PatientName";
    // + '-' +
    // tags.PatientName";
    template.update("insert into SCRIPT ( PoolKey,  Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "PatientID", script);

    List<File> testSeries = sendDICOM(aet, aet, "TOF/IMAGE001.dcm");

    DcmQR dcmQR = new DcmQR();
    dcmQR.setRemoteHost("localhost");
    dcmQR.setRemotePort(DICOMPort);
    dcmQR.setCalledAET(aet);
    dcmQR.setCalling(aet);
    dcmQR.open();

    DicomObject response = dcmQR.query();
    dcmQR.close();

    logger.info("Got response: " + response);
    assertTrue("Response was null", response != null);
    assertEquals("AccessionNumber", accessionNumber, response.getString(Tag.AccessionNumber));
    assertEquals("PatientName", patientName, response.getString(Tag.PatientName));
    assertEquals("NumberOfStudyRelatedSeries", 1, response.getInt(Tag.NumberOfStudyRelatedSeries));
    assertEquals("NumberOfStudyRelatedInstances", testSeries.size(), response.getInt(Tag.NumberOfStudyRelatedInstances));

    template.update("delete from SCRIPT where PoolKey = ?", pool.poolKey);
    script = "'42'";
    template.update("insert into SCRIPT ( PoolKey,  Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "AccessionNumber", script);
    script = "'Gone'";
    template.update("insert into SCRIPT ( PoolKey, Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "PatientName", script);

    // Send again and query
    testSeries = sendDICOM(aet, aet, "TOF/IMAGE001.dcm");
    dcmQR = new DcmQR();
    dcmQR.setRemoteHost("localhost");
    dcmQR.setRemotePort(DICOMPort);
    dcmQR.setCalledAET(aet);
    dcmQR.setCalling(aet);
    dcmQR.open();

    response = dcmQR.query();
    dcmQR.close();
    logger.info("Got response: " + response);
    assertTrue("Response was null", response != null);
    assertEquals("AccessionNumber", "42", response.getString(Tag.AccessionNumber));
    assertEquals("PatientName", "Gone", response.getString(Tag.PatientName));
    assertEquals("NumberOfStudyRelatedSeries", 1, response.getInt(Tag.NumberOfStudyRelatedSeries));
    assertEquals("NumberOfStudyRelatedInstances", testSeries.size(), response.getInt(Tag.NumberOfStudyRelatedInstances));
  }

}
