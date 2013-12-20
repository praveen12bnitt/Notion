package edu.mayo.qia.pacs.rest;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.PerRequest;

import edu.mayo.qia.pacs.components.PoolManager;

@Scope("prototype")
@Component
@PerRequest
public class SeriesEndpoint {
  static Logger logger = Logger.getLogger(ScriptEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  @Autowired
  PoolManager poolManager;

  public int poolKey;

  @GET
  public Response get(@Context UriInfo uriInfo) throws Exception {
    JSONObject json = new JSONObject();
    json.put("Result", "OK");
    // Build the query
    final JSONArray records = new JSONArray();
    json.put("Records", records);

    template.query("select * from STUDY where PoolKey = ?", new Object[] { poolKey }, new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        JSONObject row = new JSONObject();
        try {
          for (String column : new String[] { "PatientID", "PatientName", "AccessionNumber", "StudyDescription" }) {
            row.put(column, rs.getString(column));
          }
          for (String column : new String[] { "StudyKey" }) {
            row.put(column, rs.getInt(column));
          }
        } catch (JSONException e) {
          logger.error("Error setting field", e);
        }
        records.put(row);
      }
    });

    return Response.ok(json).build();
  }
}
