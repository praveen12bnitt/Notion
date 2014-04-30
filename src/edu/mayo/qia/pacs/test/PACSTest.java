package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.net.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.components.Connector;
import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.components.Script;
import edu.mayo.qia.pacs.dicom.DcmSnd;
import edu.mayo.qia.pacs.dicom.TagLoader;

@ContextConfiguration(initializers = { PACSTest.class })
public class PACSTest implements ApplicationContextInitializer<GenericApplicationContext> {
  static Logger logger = Logger.getLogger(PACSTest.class);
  static PACS pacs = null;
  static int DICOMPort = findFreePort();
  static int RESTPort = findFreePort();
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

  protected static int findFreePort() {
    // Find a random number between 49152 and 65535
    Random random = new Random();
    boolean found = false;
    int testPort = 0;
    while (!found) {
      testPort = 49152 + random.nextInt(65535 - 49152);
      Socket socket = null;
      try {
        socket = new Socket("localhost", testPort);
      } catch (IOException e) {
        found = true;
      } finally {
        if (socket != null) {
          try {
            socket.close();
          } catch (IOException e) {
            logger.error("Failed to find free port!", e);
          }
        }
      }
    }
    return testPort;
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
      args.add(Integer.toString(DICOMPort));
      args.add("-rest");
      args.add(Integer.toString(RESTPort));
      // Specify an in memory DB for testing
      args.add("-m");
      args.add(temp.getAbsolutePath());
      pacs = new PACS(args);
    }
    applicationContext.setParent(PACS.context);

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

  Connector createConnector(Connector connector) {
    // Create a device
    URI uri = UriBuilder.fromUri(baseUri).path("/connector").build();
    ClientResponse response = client.resource(uri).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, connector);
    assertEquals("Got result", 200, response.getStatus());
    connector = response.getEntity(Connector.class);
    logger.info("Entity back: " + connector);
    assertTrue("Assigned a key", connector.connectorKey != 0);
    return connector;

  }

  Script createScript(Script script) {
    // Create a device
    URI uri = UriBuilder.fromUri(baseUri).path("/pool/" + script.getPool().poolKey + "/script").build();
    ClientResponse response = client.resource(uri).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);
    assertEquals("Got result", 200, response.getStatus());
    script = response.getEntity(Script.class);
    logger.info("Entity back: " + script);
    assertTrue("Assigned a scriptKey", script.scriptKey != 0);
    return script;
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

  List<DicomObject> getTags(String pattern) throws Exception {
    return getTags(getTestSeries(pattern));
  }

  protected List<DicomObject> getTags(List<File> files) throws Exception {
    List<DicomObject> list = new ArrayList<DicomObject>();
    for (File file : files) {
      list.add(TagLoader.loadTags(file));
    }
    return list;
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
