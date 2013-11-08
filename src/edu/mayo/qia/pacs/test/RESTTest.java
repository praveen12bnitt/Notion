package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.mayo.qia.pacs.components.Pool;

@RunWith(SpringJUnit4ClassRunner.class)
public class RESTTest extends PACSTest {
  URI baseUri;
  static Client client = ClientBuilder.newClient();
  static Logger logger = Logger.getLogger(RESTTest.class);

  @Autowired
  ObjectMapper objectMapper;

  @Before
  public void setup() {
    baseUri = UriBuilder.fromUri("http://localhost/").port(RESTPort).build();
  }

  @Test
  public void status() {
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/").build();
    logger.debug("Loading: " + uri);
    response = client.target(uri).request("text/html").get(ClientResponse.class);
    assertEquals("Got result", 200, response.getStatus());
  }

  @Test
  public void createPool() throws Exception {
    Response response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    logger.debug("Loading: " + uri);
    Pool pool = new Pool("test", "test");

    response = client.target(uri).request("application/json").accept("application/json").put(Entity.json(pool));
    // put(ClientResponse.class, objectMapper.writeValueAsString(pool));
    assertEquals("Got result", 200, response.getStatus());
  }
}
