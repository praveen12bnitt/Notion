package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.dcm4che2.net.ConfigurationException;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.sun.jersey.api.client.ClientResponse;

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.dicom.DcmSnd;

public class DICOMReceiverTest extends PACSTest {
  protected List<File> getTestSeries(String resource_pattern) throws IOException {
    // Load some files
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:" + resource_pattern);
    List<File> files = new ArrayList<File>();
    for (Resource resource : resources) {
      files.add(resource.getFile());
    }
    return files;
  }

  @Test
  public void sendTOF() throws IOException, ConfigurationException, InterruptedException {
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);

    Pool pool = new Pool(aet, aet, aet);
    pool = createPool(pool);
    Device device = new Device();
    device.applicationEntityTitle = ".*";
    device.hostName = ".*";
    device.port = 1234;
    device.setPool(pool);
    device = createDevice(device);

    // Send the files
    DcmSnd sender = new DcmSnd();
    // Startup the sender
    sender.setRemoteHost("localhost");
    sender.setRemotePort(DICOMPort);
    sender.setCalledAET(aet);
    sender.setCalling(aet);
    List<File> testSeries = getTestSeries("TOF/*.dcm");
    for (File f : testSeries) {
      if (f.isFile()) {
        sender.addFile(f);
      }
    }
    sender.configureTransferCapability();
    sender.start();
    sender.open();
    sender.send();
    sender.close();
    sender.stop();

    int studyCount = template.queryForObject("select count(*) from Study where PoolKey = ?", new Object[] { pool.poolKey }, Integer.class);
    assertEquals("StudyCount", 1, studyCount);
    int seriesCount = template.queryForObject("select count(Series.SeriesKey) from Study, Series where Study.PoolKey = ? and Series.StudyKey = Study.StudyKey", new Object[] { pool.poolKey }, Integer.class);
    assertEquals("Series Count", 2, seriesCount);
    int instanceCount = template.queryForObject("select count(Instance.InstanceKey) from Instance, Study, Series where Instance.SeriesKey = Series.SeriesKey and Study.PoolKey = ? and Series.StudyKey = Study.StudyKey", new Object[] { pool.poolKey },
        Integer.class);
    assertEquals("Instance Count", testSeries.size(), instanceCount);

  }
}
