package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.net.ConfigurationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.ctp.Anonymizer;
import edu.mayo.qia.pacs.dicom.DcmQR;
import edu.mayo.qia.pacs.dicom.TagLoader;

@RunWith(SpringJUnit4ClassRunner.class)
public class AnonymizerTest extends PACSTest {

  @Autowired
  Anonymizer anonymizer;

  @Test
  public void anonymizeBasics() throws Exception {

    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    String accessionNumber = "AccessionNumber-1234";
    String script = "'" + accessionNumber + "'";
    template.update("insert into SCRIPT ( PoolKey,  Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "AccessionNumber", script);

    String patientName = "PN-1234";
    script = "'" + patientName + "'";
    template.update("insert into SCRIPT ( PoolKey,  Tag, Script ) values ( ?, ?, ? )", pool.poolKey, "PatientName", script);

    String patientID = "MRA-0068-MRA-0068";
    script = "tags.PatientID + '-' + tags.PatientsName";
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
    assertEquals("PatientID", patientID, response.getString(Tag.PatientID));
    assertEquals("NumberOfStudyRelatedSeries", 1, response.getInt(Tag.NumberOfStudyRelatedSeries));
    assertEquals("NumberOfStudyRelatedInstances", testSeries.size(), response.getInt(Tag.NumberOfStudyRelatedInstances));

  }

  @Test
  public void sequences() throws Exception {
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet);
    pool = createPool(pool);

    anonymizer.setPool(pool);

    assertEquals("Unset value", null, anonymizer.lookup("PatientID", "Jones"));
    anonymizer.setValue("PatientID", "Jones", "1234");
    assertEquals("Set value", "1234", anonymizer.lookup("PatientID", "Jones"));
    assertEquals("Sequence", 1, anonymizer.sequenceNumber("PatientID", "Jones"));
    assertEquals("Asked for same sequence number", 1, anonymizer.sequenceNumber("PatientID", "Jones"));
    assertEquals("Should increment sequence number", 2, anonymizer.sequenceNumber("PatientID", "Smith"));
  }

}
