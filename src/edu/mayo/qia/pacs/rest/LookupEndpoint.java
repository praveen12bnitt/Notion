package edu.mayo.qia.pacs.rest;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.sun.jersey.spi.resource.PerRequest;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.ctp.Anonymizer;

@Scope("prototype")
@Component
@PerRequest
public class LookupEndpoint extends TableEndpoint {
  static Logger logger = Logger.getLogger(LookupEndpoint.class);

  @Autowired
  TransactionTemplate transactionTemplate;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo) throws Exception {
    JSONObject json = super.get(uriInfo.getQueryParameters(), "LOOKUP", " and Visible = true ", new String[] { "Type", "Name", "Value" }, "LookupKey");
    return Response.ok(json).build();
  }

  @POST
  @Path("/create")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createRecord(@Context UriInfo uriInfo, @FormParam("Type") String type, @FormParam("Name") String name, @FormParam("Value") String value) throws Exception {
    logger.info("Create: " + type + "/" + name + "/" + value);
    Anonymizer anonymizer = PACS.context.getBean("anonymizer", Anonymizer.class);
    anonymizer.setPool(poolManager.getContainer(poolKey).getPool());
    JSONObject json;
    // Does it already exist?
    Object[] k = anonymizer.lookupValueAndKey(type, name);
    if (k[0] != null) {
      // Already exists
      json = new JSONObject();
      json.put("Result", "ERROR");
      json.put("Message", "A lookup value for " + type + "/" + name + " already exists");
      return Response.ok(json).build();
    }

    int lookupKey = anonymizer.setValue(type, name, value);
    json = super.get(uriInfo.getQueryParameters(), "LOOKUP", " and Visible = true and LookupKey = " + lookupKey, new String[] { "Type", "Name", "Value" }, "LookupKey");
    // Change Records into Record!
    json.put("Record", json.getJSONArray("Records").get(0));
    json.remove("TotalRecordCount");
    json.remove("Records");
    return Response.ok(json).build();
  }

  @POST
  @Path("/update")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createRecord(@Context UriInfo uriInfo, @FormParam("LookupKey") final Integer lookupKey, @FormParam("Type") final String type, @FormParam("Name") final String name, @FormParam("Value") final String value) throws Exception {
    logger.info("Update: " + lookupKey + "/" + type + "/" + name + "/" + value);
    Anonymizer anonymizer = PACS.context.getBean("anonymizer", Anonymizer.class);
    anonymizer.setPool(poolManager.getContainer(poolKey).getPool());
    JSONObject json;
    // Does it already exist?
    int count = template.queryForObject("select count(*) from LOOKUP where PoolKey = ? and LookupKey = ?", new Object[] { poolKey, lookupKey }, Integer.class);
    if (count != 1) {
      // Already exists
      json = new JSONObject();
      json.put("Result", "ERROR");
      json.put("Message", "A lookup value does not exist");
      return Response.ok(json).build();
    }
    transactionTemplate.execute(new TransactionCallback<JSONObject>() {
      @Override
      public JSONObject doInTransaction(TransactionStatus status) {
        int count = template.update("update LOOKUP set Name = ?, Value = ? where PoolKey = ? and LookupKey = ?", name, value, poolKey, lookupKey);
        JSONObject json = new JSONObject();
        try {
          if (count == 1) {
            json.put("Result", "OK");
          } else {
            status.setRollbackOnly();
            // Already exists
            json.put("Result", "ERROR");
            json.put("Message", "Updated failed to update exactly one row");
          }
        } catch (Exception e) {
          logger.error("Error setting JSON fields", e);
        }
        return json;
      }
    });

    json = new JSONObject();
    json.put("Result", "OK");
    return Response.ok(json).build();
  }

  @POST
  @Path("/delete")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteRecord(@FormParam("LookupKey") String lookupKey) throws Exception {
    JSONObject json;

    int rows = template.update("delete from LOOKUP where PoolKey = ? and LookupKey = ?", new Object[] { poolKey, lookupKey });
    if (rows == 1) {
      json = new JSONObject();
      json.put("Result", "OK");
    } else {
      json = new JSONObject();
      json.put("Result", "ERROR");
      json.put("Message", "Delete failed, deleted " + rows + " rows");
    }
    return Response.ok(json).build();
  }
}
