package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

@RunWith(SpringJUnit4ClassRunner.class)
public class RESTTest extends PACSTest {
  URI baseUri;
  static Client client = Client.create();
  static Logger logger = Logger.getLogger(RESTTest.class);

  @Before
  public void setup() {
    baseUri = UriBuilder.fromUri("http://localhost/").port(RESTPort).build();
  }

  @Test
  public void base() {
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/").build();
    logger.debug("Loading: " + uri);
    response = client.resource(uri).accept("text/html").get(ClientResponse.class);
    assertEquals("Got result", 200, response.getStatus());
  }
}
