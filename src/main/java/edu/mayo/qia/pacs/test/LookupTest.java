package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.representation.Form;

import edu.mayo.qia.pacs.components.Pool;

@RunWith(SpringJUnit4ClassRunner.class)
public class LookupTest extends PACSTest {

  @Test
  public void saveLookup() throws Exception {
    UUID uid = UUID.randomUUID();
    String aet = uid.toString().substring(0, 10);
    Pool pool = new Pool(aet, aet, aet, false);
    pool = createPool(pool);

    ClientResponse response = null;
    URI uri;

    // Create
    uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/lookup").path("create").build();
    Form form = new Form();
    form.add("Type", "PatientName");
    form.add("Name", "Mr. Magoo");
    form.add("Value", "Rikki-tikki-tavvi");
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, form);
    assertEquals("Status", 200, response.getStatus());
    assertEquals("Count", new Integer(1), template.queryForObject("select count(*) from LOOKUP where PoolKey = " + pool.poolKey, Integer.class));

    JSONObject json = response.getEntity(JSONObject.class);
    assertEquals("Result", "OK", json.getString("Result"));
    assertTrue("Record", json.has("Record"));
    int lookupKey = json.getJSONObject("Record").getInt("LookupKey");
    assertEquals("Value", "Rikki-tikki-tavvi", template.queryForObject("select Value from LOOKUP where LookupKey = ?", new Object[] { lookupKey }, String.class));

    // Modify the record
    form.putSingle("LookupKey", lookupKey);
    form.putSingle("Value", "Darth Vader");
    uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/lookup").path("update").build();
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, form);
    assertEquals("Status", 200, response.getStatus());
    assertEquals("Value", "Darth Vader", template.queryForObject("select Value from LOOKUP where LookupKey = ?", new Object[] { lookupKey }, String.class));

    // Query
    uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/lookup").build();
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class);
    assertEquals("Status", 200, response.getStatus());
    json = response.getEntity(JSONObject.class);
    assertEquals("Count", 1, json.getJSONArray("Records").length());
    assertEquals("LookupKey", lookupKey, json.getJSONArray("Records").getJSONObject(0).getInt("LookupKey"));

    // Delete
    uri = UriBuilder.fromUri(baseUri).path("/pool/" + pool.poolKey + "/lookup").path("delete").build();
    response = client.resource(uri).type(JSON).accept(JSON).post(ClientResponse.class, form);
    assertEquals("Status", 200, response.getStatus());
    json = response.getEntity(JSONObject.class);
    assertEquals("Result", "OK", json.getString("Result"));

    //

  }
}
