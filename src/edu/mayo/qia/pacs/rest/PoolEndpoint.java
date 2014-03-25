package edu.mayo.qia.pacs.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableBiMap.Builder;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.core.ResourceContext;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.components.MoveRequest;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.components.PoolContainer;
import edu.mayo.qia.pacs.components.PoolManager;
import edu.mayo.qia.pacs.components.Script;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Path("/pool")
@Scope("singleton")
public class PoolEndpoint {
  static Logger logger = Logger.getLogger(PoolEndpoint.class);

  @Autowired
  JdbcTemplate template;

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  PoolManager poolManager;

  @Context
  ResourceContext resourceContext;

  /** List all the pools */
  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response listPools() {
    List<Pool> result = new ArrayList<Pool>();
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      result = session.createCriteria(Pool.class).list();
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    SimpleResponse s = new SimpleResponse("pool", result);
    return Response.ok(s).build();
  }

  /** Get a pool. */
  @GET
  @Path("/{id: [1-9][0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPool(@PathParam("id") int id) {
    // Look up the pool and change it
    Pool pool = null;
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      pool = (Pool) session.byId(Pool.class).load(id);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok(pool).build();
  }

  @GET
  @Path("/{id: [1-9][0-9]*}/statistics")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPoolStatistics(@PathParam("id") int id) {
    SimpleResponse s = new SimpleResponse();
    s.put("study", template.queryForObject("select count(STUDY.StudyKey) from STUDY where STUDY.PoolKey = ?", Integer.class, id));
    s.put("series", template.queryForObject("select count(SERIES.SeriesKey) from SERIES, STUDY where STUDY.PoolKey = ? and SERIES.StudyKey = STUDY.StudyKey", Integer.class, id));
    s.put("instance", template.queryForObject("select count(INSTANCE.InstanceKey) from INSTANCE, SERIES, STUDY where STUDY.PoolKey = ? and SERIES.StudyKey = STUDY.StudyKey and INSTANCE.SeriesKey = SERIES.SeriesKey", Integer.class, id));
    return Response.ok(s).build();
  }

  @PUT
  @Path("/{id: [1-9][0-9]*}/move")
  @Produces(MediaType.APPLICATION_JSON)
  public Response moveStudies(@PathParam("id") int id, MoveRequest request) throws JSONException {
    JSONObject json = new JSONObject();
    // Build the query
    final JSONArray records = new JSONArray();
    PoolContainer destinationContainer = poolManager.getContainer(request.destinationPoolKey);
    PoolContainer container = poolManager.getContainer(id);
    if (destinationContainer == null || container == null) {
      return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("Message", "Could not find pool")).build();
    }
    // Find all the files and import
    for (int studyKey : request.studyKeys) {
      JSONObject studyResult = new JSONObject();
      try {
        List<String> filePaths = template.queryForList("select FilePath from INSTANCE, SERIES, STUDY where INSTANCE.SeriesKey = SERIES.SeriesKey and SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = ? and STUDY.StudyKey = ?", new Object[] { id,
            studyKey }, String.class);
        for (String file : filePaths) {
          destinationContainer.importFromPool(new File(container.getPoolDirectory(), file));
        }
        studyResult.put("Status", "success");
        studyResult.put("Message", "success");
      } catch (Exception e) {
        studyResult.put("Status", "failed");
        studyResult.put("Message", e.getLocalizedMessage());
      }
      records.put(studyResult);
    }
    json.put("Status", records);
    json.put("Result", "OK");
    return Response.ok(json).build();
  }

  /** Devices */
  @Path("/{id: [1-9][0-9]*}/device")
  public DeviceEndpoint devices(@PathParam("id") int id) {
    DeviceEndpoint deviceEndpoint;
    deviceEndpoint = resourceContext.getResource(DeviceEndpoint.class);
    deviceEndpoint.poolKey = id;
    return deviceEndpoint;
  }

  /** Series */
  @Path("/{id: [1-9][0-9]*}/studies")
  public StudiesEndpoint studies(@PathParam("id") int id) {
    StudiesEndpoint seriesEndpoint;
    seriesEndpoint = resourceContext.getResource(StudiesEndpoint.class);
    seriesEndpoint.poolKey = id;
    return seriesEndpoint;
  }

  /** Lookup */
  @Path("/{id: [1-9][0-9]*}/lookup")
  public LookupEndpoint lookup(@PathParam("id") int id) {
    LookupEndpoint endpoint;
    endpoint = resourceContext.getResource(LookupEndpoint.class);
    endpoint.poolKey = id;
    return endpoint;
  }

  /** Scripts */
  @Path("/{id: [1-9][0-9]*}/script")
  public ScriptEndpoint scripts(@PathParam("id") int id) {
    ScriptEndpoint scriptEndpoint;
    scriptEndpoint = resourceContext.getResource(ScriptEndpoint.class);
    scriptEndpoint.poolKey = id;
    return scriptEndpoint;
  }

  /** Query */
  @Path("/{id: [1-9][0-9]*}/query")
  public QueryEndpoint query(@PathParam("id") int id) {
    QueryEndpoint queryEndpoint;
    queryEndpoint = resourceContext.getResource(QueryEndpoint.class);
    queryEndpoint.poolKey = id;
    return queryEndpoint;
  }

  /** CTP */
  @GET
  @Path("/{id: [1-9][0-9]*}/ctp")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCTPConfig(@PathParam("id") int id) {
    PoolContainer poolContainer = poolManager.getContainer(id);
    if (poolContainer == null) {
      return Response.status(Response.Status.FORBIDDEN).entity(new SimpleResponse("message", "Unknown pool")).build();
    }
    try {
      String config = poolContainer.getCTPConfig();
      return Response.ok(new SimpleResponse("script", config)).build();
    } catch (Exception e) {
      logger.error("Failed to read config file for pool", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new SimpleResponse("message", "Failed to read CTP config file for pool")).build();
    }
  }

  /** CTP */
  @PUT
  @Path("/{id: [1-9][0-9]*}/ctp")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response saveCTPConfig(@PathParam("id") int id, JsonNode json) {
    PoolContainer poolContainer = poolManager.getContainer(id);
    if (poolContainer == null || !json.has("script")) {
      return Response.status(Response.Status.FORBIDDEN).entity(new SimpleResponse("message", "Unknown pool")).build();
    }
    try {
      poolContainer.putCTPConfig(json.get("script").asText());
      return Response.ok(new SimpleResponse("script", json.get("script").asText())).build();
    } catch (Exception e) {
      logger.error("Failed to save config file for pool", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new SimpleResponse("message", "Failed to save CTP config file for pool")).build();
    }
  }

  /** Create a pool. */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createPool(Pool pool) {

    if (pool.name == null || pool.description == null || pool.applicationEntityTitle == null) {
      return Response.status(Response.Status.FORBIDDEN).entity(new SimpleResponse("message", "ApplicationEntityTitle, Name and Description must not be empty")).build();
    }

    // Does the name conform to what we expect?
    if (!pool.applicationEntityTitle.matches("[a-zA-Z_\\-0-9]+")) {
      return Response.status(Response.Status.FORBIDDEN).entity(new SimpleResponse("message", "ApplicationEntityTitle must consist of letters, numbers dashes, and underscores only")).build();
    }
    // Does the name conform to what we expect?
    if (pool.applicationEntityTitle.length() > 16) {
      return Response.status(Response.Status.FORBIDDEN).entity(new SimpleResponse("message", "ApplicationEntityTitle must be less than 16 characters: " + pool.applicationEntityTitle + " is " + pool.applicationEntityTitle.length() + " characters long"))
          .build();
    }

    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      Script s;
      s = new Script("PatientName", Script.createDefaultScript("PatientName", "PN-"));
      pool.scripts.add(s);
      s.setPool(pool);
      s = new Script("PatientID", Script.createDefaultScript("PatientID", null));
      pool.scripts.add(s);
      s.setPool(pool);

      session.save(pool);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    poolManager.newPool(pool);
    return Response.ok(pool).build();
  }

  /** Modify a pool. */
  @PUT
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifyPool(@PathParam("id") int id, Pool update) {
    // Look up the pool and change it
    Pool pool = null;
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      pool = (Pool) session.byId(Pool.class).load(id);
      if (pool == null) {
        return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("message", "Could not load the pool")).build();
      }
      session.getTransaction().commit();
      if (!pool.applicationEntityTitle.equals(update.applicationEntityTitle)) {
        return Response.status(Response.Status.FORBIDDEN).entity(new SimpleResponse("message", "ApplicationEntityTitle can not be changed existing: " + pool.applicationEntityTitle + " attepted change: " + update.applicationEntityTitle)).build();

      }
      // Update the pool
      session.beginTransaction();
      pool.update(update);
      session.update(pool);
      session.getTransaction().commit();
      poolManager.update(pool);
    } finally {
      session.close();
    }

    return Response.ok(pool).build();
  }

  /** Delete a pool. */
  @DELETE
  @Path("/{id: [1-9][0-9]*}")
  public Response modifyPool(@PathParam("id") int id) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      Pool pool = (Pool) session.byId(Pool.class).load(id);
      if (pool == null) {
        return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("message", "Could not load the pool")).build();
      }
      // Delete
      session.delete(pool);
      poolManager.deletePool(pool);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok().build();
  }
}
