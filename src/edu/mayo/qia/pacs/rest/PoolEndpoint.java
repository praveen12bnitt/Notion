package edu.mayo.qia.pacs.rest;

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

import org.apache.log4j.Logger;
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
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.components.PoolManager;

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

  /** Devices */
  @Path("/{id: [1-9][0-9]*}/device")
  public DeviceEndpoint devices(@PathParam("id") int id) {
    DeviceEndpoint deviceEndpoint;
    deviceEndpoint = resourceContext.getResource(DeviceEndpoint.class);
    deviceEndpoint.poolKey = id;
    return deviceEndpoint;
  }

  /** Devices */
  @Path("/{id: [1-9][0-9]*}/script")
  public ScriptEndpoint scripts(@PathParam("id") int id) {
    ScriptEndpoint scriptEndpoint;
    scriptEndpoint = resourceContext.getResource(ScriptEndpoint.class);
    scriptEndpoint.poolKey = id;
    return scriptEndpoint;
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
    Session session = sessionFactory.getCurrentSession();
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
