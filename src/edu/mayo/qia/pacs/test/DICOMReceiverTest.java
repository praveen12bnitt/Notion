package edu.mayo.qia.pacs.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che2.net.ConfigurationException;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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
    // Send the files
    DcmSnd sender = new DcmSnd();
    // Startup the sender
    sender.setRemoteHost("localhost");
    sender.setRemotePort(DICOMPort);
    sender.setCalledAET("default");
    sender.setCalling("default");
    for (File f : getTestSeries("TOF/*dcm")) {
      if (f.isFile()) {
        sender.addFile(f);
        break;
      }
    }
    sender.configureTransferCapability();
    sender.start();
    sender.open();
    sender.send();
    sender.close();
    sender.stop();
  }

}
