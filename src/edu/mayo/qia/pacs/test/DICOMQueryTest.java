package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.NetworkConnection;
import org.junit.Test;

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.dicom.DcmMoveException;
import edu.mayo.qia.pacs.dicom.DcmQR;
import edu.mayo.qia.pacs.dicom.TagLoader;

public class DICOMQueryTest extends PACSTest {

  @Test
  public void query() throws Exception {

    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    aet = "test";
    Pool pool = new Pool(aet, aet, aet);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    List<File> testSeries = sendDICOM(aet, aet, "TOF/*.dcm");
    DicomObject tags = TagLoader.loadTags(testSeries.get(0));

    DcmQR dcmQR = new DcmQR();
    dcmQR.setRemoteHost("localhost");
    dcmQR.setRemotePort(DICOMPort);
    dcmQR.setCalledAET(aet);
    dcmQR.setCalling(aet);
    dcmQR.open();

    DicomObject response = dcmQR.query();
    dcmQR.close();

    for (String tag : new String[] { "AccessionNumber", "PatientName", "PatientID" }) {
      assertEquals(tag, tags.getString(Tag.forName(tag)), response.getString(Tag.forName(tag)));
    }
    for (String tag : new String[] { "StudyInstanceUID", "StudyID", "AccessionNumber", "PatientName", "PatientID" }) {
      assertEquals(tag, tags.getString(Tag.forName(tag)), response.getString(Tag.forName(tag)));
    }
    assertEquals("NumberOfStudyRelatedSeries", 2, response.getInt(Tag.NumberOfStudyRelatedSeries));
    assertEquals("NumberOfStudyRelatedInstances", testSeries.size(), response.getInt(Tag.NumberOfStudyRelatedInstances));

  }
}
