package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import edu.mayo.qia.pacs.PACS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = { PACSTest.class })
public class PACSTest implements ApplicationContextInitializer<GenericApplicationContext> {
  Logger logger = Logger.getLogger(PACSTest.class);
  static PACS pacs = null;
  static final int DICOMPort = 12345;
  static final int RESTPort = 12346;
  static Client client;

  static URI baseUri = UriBuilder.fromUri("http://localhost/").port(RESTPort).build();
  static final String JSON = MediaType.APPLICATION_JSON;

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
      String[] args = { "-port", "12345", "-rest", "12346", temp.getAbsolutePath() };
      pacs = new PACS(args);
    }
    applicationContext.setParent(PACS.context);
  }

  @Test
  public void configuration() {
    assertTrue(PACS.context != null);
  }

}
