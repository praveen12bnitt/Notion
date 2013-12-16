package edu.mayo.qia.pacs.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.spi.resource.PerRequest;

import edu.mayo.qia.pacs.components.Script;
import edu.mayo.qia.pacs.components.Pool;

@Scope("prototype")
@Component
@PerRequest
public class ScriptEndpoint {

  static Logger logger = Logger.getLogger(ScriptEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  public int poolKey;

  /** List all the Scripts. */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response listScripts() {
    // Look up the pool and change it
    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    Pool pool = (Pool) session.byId(Pool.class).load(poolKey);
    // Force load
    pool.getScripts().size();
    session.getTransaction().commit();
    SimpleResponse s = new SimpleResponse("Script", pool.getScripts());
    return Response.ok(s).build();
  }

  @GET
  @Path("/{id: [1-9][0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getScript(@PathParam("id") int id) {
    // Look up the pool and change it
    Script script = null;
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      script = (Script) session.byId(Script.class).load(id);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok(script).build();
  }

  /** Create a Script. */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createScript(Script script) {
    // Ensure this Script does not already exist
    int count = template.queryForObject("select count(*) from SCRIPT where PoolKey = ? and Tag = ?", Integer.class, poolKey, script.tag);
    if (count != 0) {
      return Response.status(Status.FORBIDDEN).entity(new SimpleResponse("message", "Script tag (" + script.tag + ") must be unique for this pool")).build();
    }
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      Pool pool = (Pool) session.byId(Pool.class).load(poolKey);
      // Ensure the Script does not think it already exists!
      script.scriptKey = -1;
      script.setPool(pool);
      pool.getScripts().add(script);
      session.getTransaction().commit();
    } catch (Exception e) {
      logger.error("Error creating Script", e);
    } finally {
      session.close();
    }
    return Response.ok(script).build();
  }

  /** Update a Script. */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifyScript(Script inScript) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    Script script = inScript;
    try {
      session.beginTransaction();
      script = (Script) session.byId(Script.class).getReference(script.scriptKey);
      script.update(inScript);
      session.getTransaction().commit();
    } catch (Exception e) {
      logger.error("Error creating Script", e);
      return Response.status(Status.FORBIDDEN).entity(new SimpleResponse("message", "Could not update script")).build();
    } finally {
      session.close();
    }
    return Response.ok(script).build();
  }

  /** Delete a Script. */
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteScript(Script Script) {
    // Look up the pool and change it
    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    Script = (Script) session.byId(Script.class).getReference(Script.scriptKey);
    session.delete(Script);
    session.getTransaction().commit();
    Map<String, String> response = new HashMap<String, String>();
    response.put("status", "success");
    response.put("message", "Delete Script " + Script.scriptKey);
    return Response.ok().build();
  }

}
