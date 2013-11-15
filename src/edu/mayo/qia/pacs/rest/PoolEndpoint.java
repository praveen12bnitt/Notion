package edu.mayo.qia.pacs.rest;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.persistence.PersistenceContext;
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

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.inject.Inject;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.SessionManager;
import edu.mayo.qia.pacs.components.Pool;

@Component
@Path("/pool")
public class PoolEndpoint {
  static Logger logger = Logger.getLogger(PoolEndpoint.class);
  Pool myPool;

  @Context
  Integer myInteger;

  @Context
  ResourceConfig resourceConfig;

  @Autowired
  JdbcTemplate template;

  @Autowired
  SessionFactory sessionFactory;

  /** List all the pools */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Pool> listPools() {
    logger.error("My Customing wiring is: " + myInteger);
    logger.error("Tried to wire this at " + resourceConfig);
    logger.error("mpPool  " + myPool);
    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    List<Pool> result = session.createCriteria(Pool.class).list();
    session.getTransaction().commit();
    return result;
  }

  /** Devices */
  @Path("/{id: [1-9][0-9]*}/devices")
  public DeviceEndpoint devices(@PathParam("id") int id) {
    Session session = sessionFactory.getCurrentSession();
    DeviceEndpoint device = PACS.context.getBean(DeviceEndpoint.class);
    device.pool = (Pool) session.byId(Pool.class).getReference(id);
    return device;
  }

  /** Create a pool. */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createPool(Pool pool) {

    // Does the name conform to what we expect?
    if (!pool.name.matches("\\w+")) {
      return Response.status(Response.Status.FORBIDDEN).entity("Pool name must consist of letters, numbers and underscores only").build();
    }
    Session session = sessionFactory.openSession();
    session.beginTransaction();
    session.save(pool);
    session.getTransaction().commit();
    session.close();
    return Response.ok(pool).build();
  }

  /** Modify a pool. */
  @PUT
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifyPool(@PathParam("id") int id, Pool update) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    session.beginTransaction();
    Pool pool = (Pool) session.byId(Pool.class).getReference(id);
    // Update the pool
    pool.update(update);
    session.update(pool);
    session.getTransaction().commit();
    session.close();
    return Response.ok(pool).build();
  }

  /** Delete a pool. */
  @DELETE
  @Path("/{id: [1-9][0-9]*}")
  public Response modifyPool(@PathParam("id") int id) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    session.beginTransaction();
    Pool pool = (Pool) session.byId(Pool.class).getReference(id);
    // Delete
    session.delete(pool);
    session.getTransaction().commit();
    session.close();
    return Response.ok().build();
  }
}
