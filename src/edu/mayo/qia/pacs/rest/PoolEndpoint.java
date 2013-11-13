package edu.mayo.qia.pacs.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.components.Pool;

@Component
@Path("/pool")
public class PoolEndpoint {

  @Autowired
  JdbcTemplate template;

  @Autowired
  SessionFactory sessionFactory;

  /** List all the pools */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Pool> listPools() {
    Session session = sessionFactory.openSession();
    session.beginTransaction();
    List<Pool> result = session.createCriteria(Pool.class).list();
    return result;
  }

  /** Create a pool. */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
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
}
