package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.representation.Form;

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.rest.SimpleResponse;

@RunWith(SpringJUnit4ClassRunner.class)
public class StudiesTest extends PACSTest {

  @Test
  public void fetchStudiesList() throws Exception {
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, false);
    pool = createPool(pool);
    Device device = new Device(".*", ".*", 1234, pool);
    device = createDevice(device);

    sendDICOM(aet, aet, "TOF/*.dcm");
    assertEquals("DB", new Integer(1), template.queryForObject("select count(*) from STUDY where PoolKey = " + pool.poolKey, Integer.class));

    ClientResponse response = null;
    URI uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/studies").build();
    ObjectNode json = new ObjectMapper().createObjectNode();
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, json);
    assertEquals("Got result", 200, response.getStatus());
    json = response.getEntity(ObjectNode.class);
    assertTrue("Result", json.has("Result"));
    assertTrue("Records", json.has("Records"));
    assertEquals("Count", 1, json.withArray("Records").size());
    assertEquals("Entry", "MRA-0068", json.withArray("Records").get(0).get("PatientID").textValue());

    // Delete, need to use the Form object
    Form f = new Form();
    f.add("StudyKey", json.withArray("Records").get(0).get("StudyKey").textValue());
    uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/studies/delete").build();
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, f);
    assertTrue("Result", json.has("Result"));
    assertEquals("Result OK", "OK", json.get("Result").textValue());

    // Query the DB
    assertEquals("DB", new Integer(0), template.queryForObject("select count(*) from STUDY where PoolKey = " + pool.poolKey, Integer.class));

  }
}
