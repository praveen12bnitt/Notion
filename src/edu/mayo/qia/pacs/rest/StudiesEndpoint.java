package edu.mayo.qia.pacs.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
public class StudiesEndpoint {
  static Logger logger = Logger.getLogger(ScriptEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  @Autowired
  PoolManager poolManager;

  public int poolKey;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo) throws Exception {
    final Set<String> columns = new HashSet<String>(Arrays.asList(new String[] { "PatientID", "PatientName", "AccessionNumber", "StudyDescription" }));
    final Set<String> directions = new HashSet<String>();
    directions.add("ASC");
    directions.add("DESC");
    JSONObject json = new JSONObject();
    json.put("Result", "OK");
    // Build the query
    final JSONArray records = new JSONArray();
    json.put("Records", records);
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

    StringBuilder query = new StringBuilder("select * from STUDY where PoolKey = ?");
    ArrayList<Object> parameters = new ArrayList<Object>();
    parameters.add(poolKey);

    if (queryParameters.containsKey("jtSorting")) {
      query.append(" ORDER BY ");

      for (String clause : queryParameters.getFirst("jtSorting").split(",")) {
        String[] p = clause.split("\\s+");
        if (columns.contains(p[0])) {
          query.append(p[0]);
        }
        if (directions.contains(p[1])) {
          query.append(" " + p[1]);
        }
      }
    }

    // jtStartIndex: Start index of records for current page.
    // jtPageSize: Count of maximum expected records.
    if (queryParameters.containsKey("jtStartIndex") && queryParameters.containsKey("jtPageSize")) {
      query.append(" OFFSET ? ROWS ");
      parameters.add(queryParameters.getFirst("jtStartIndex"));
      query.append(" FETCH NEXT ? ROWS ONLY ");
      parameters.add(queryParameters.getFirst("jtPageSize"));

    }
    // Also need to return the total number of records
    json.put("TotalRecordCount", template.queryForObject("select count(*) from STUDY", Integer.class));

    template.query(query.toString(), parameters.toArray(), new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        JSONObject row = new JSONObject();
        try {
          for (String column : columns) {
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

  /** Delete a Study. */
  @DELETE
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteScript(@PathParam("id") int id) {
    if (poolManager.getContainer(poolKey) != null) {
      poolManager.getContainer(poolKey).deleteStudy(id);
    }
    SimpleResponse response = new SimpleResponse();
    response.put("status", "success");
    response.put("message", "Delete study " + id);
    return Response.ok(response).build();
  }

  @POST
  @Path("/delete")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteRecord(MultivaluedMap<String, String> formParams, @FormParam("StudyKey") int studyKey) {
    logger.info("Form Parameters: " + formParams);
    poolManager.getContainer(poolKey).deleteStudy(studyKey);
    return Response.ok(new SimpleResponse("Result", "OK")).build();
  }
}
