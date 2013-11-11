package edu.mayo.qia.pacs.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.components.Pool;

@Component
@Path("/pool")
public class PoolEndpoint {

  @Autowired
  JdbcTemplate template;

  /** List all the pools */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response listPools() {
    return Response.ok(new Pool("test", "value")).build();
  }

  /** Create a pool. */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createPool(Pool pool) {
    // Does the pool exist?
    Integer count = template.queryForObject("select count(*) from POOL where Name = ?", Integer.class, pool.getName());
    if (count != 0) {
      return Response.status(Response.Status.FORBIDDEN).entity("Forbidden").build();
    }
    return Response.status(Response.Status.FORBIDDEN).entity("Forbidden").build();
  }
}
