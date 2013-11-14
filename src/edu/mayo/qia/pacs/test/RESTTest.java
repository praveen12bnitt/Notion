package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.ClientResponse;

import edu.mayo.qia.pacs.components.Pool;

@RunWith(SpringJUnit4ClassRunner.class)
public class RESTTest extends PACSTest {
  URI baseUri;
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

  @Test
  public void createPool() {
    // CURL Code
    /*
     * curl -X POST -H "Content-Type: application/json" -d
     * '{"name":"foo","path":"bar"}' http://localhost:11118/pool
     */
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    Pool pool = new Pool("empty", "empty");
    response = client.resource(uri).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, pool);
    assertEquals("Got result", 200, response.getStatus());
    pool = response.getEntity(Pool.class);
    logger.info("Entity back: " + pool);
    assertTrue("Assigned an id", pool.poolKey != 0);
  }

  @Test
  public void invalidPoolName() {
    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool").build();
    for (String name : new String[] { "no spaces", "no !", "{", "#" }) {
      Pool pool = new Pool(name, "empty");
      response = client.resource(uri).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, pool);
      assertEquals("Got result", Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
  }
}
