package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dcm4che2.net.ConfigurationException;
import org.h2.tools.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.dicom.DcmSnd;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = { PACSTest.class })
public class PACSTest implements ApplicationContextInitializer<GenericApplicationContext> {
  Logger logger = Logger.getLogger(PACSTest.class);
  static PACS pacs = null;
  static final int DICOMPort = 12345;
  static final int RESTPort = 12346;
  static Client client;

  static URI baseUri = UriBuilder.fromUri("http://localhost/").port(RESTPort).path("rest").build();
  static final String JSON = MediaType.APPLICATION_JSON;

  @Autowired
  JdbcTemplate template;

  static {
    ClientConfig config = new DefaultClientConfig();
    config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(config);
  }

  @Override
  public synchronized void initialize(GenericApplicationContext applicationContext) {
    if (pacs == null) {
      // Clean out the temp directory
      File temp = new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name"));
      try {
        FileUtils.deleteDirectory(temp);
      } catch (IOException e) {
        logger.error("Error cleaning up directory", e);
      }
      List<String> args = new ArrayList<String>();
      if (System.getenv("bamboo.buildKey") == null) {
        args.add("-db");
        args.add("8084");
      }
      args.add("-port");
      args.add("12345");
      args.add("-rest");
      args.add("12346");
      args.add(temp.getAbsolutePath());
      pacs = new PACS(args);
    }
    applicationContext.setParent(PACS.context);

  }

  @Test
  public void configuration() {
    assertTrue(PACS.context != null);
  }

  Pool createPool(Pool pool) {
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, pool);
    assertEquals("Got result", 200, response.getStatus());
    pool = response.getEntity(Pool.class);
    assertTrue("Assigned an id", pool.poolKey != 0);
    return pool;

  }

  Device createDevice(Device device) {
    // Create a device
    URI uri = UriBuilder.fromUri(baseUri).path("/pool/" + device.getPool().poolKey + "/device").build();
    ClientResponse response = client.resource(uri).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, device);
    assertEquals("Got result", 200, response.getStatus());
    device = response.getEntity(Device.class);
    logger.info("Entity back: " + device);
    assertTrue("Assigned a deviceKey", device.deviceKey != 0);
    return device;

  }

  protected List<File> sendDICOM(String called, String calling, String series) throws IOException, ConfigurationException, InterruptedException {
    // Send the files
    DcmSnd sender = new DcmSnd("test");
    // Startup the sender
    sender.setRemoteHost("localhost");
    sender.setRemotePort(DICOMPort);
    sender.setCalledAET(called);
    sender.setCalling(calling);
    List<File> testSeries = getTestSeries(series);
    for (File f : testSeries) {
      if (f.isFile()) {
        sender.addFile(f);
      }
    }
    sender.configureTransferCapability();
    sender.open();
    sender.send(null);
    sender.close();
    return testSeries;
  }

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

}
