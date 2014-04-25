package edu.mayo.qia.pacs.rest;

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

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;

// Create a new one every time
/*
 @Component
 */
@Scope("prototype")
@Component
@PerRequest
public class DeviceEndpoint {
  static Logger logger = Logger.getLogger(DeviceEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  public int poolKey;

  /** List all the devices. */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response listDevices() {
    // Look up the pool and change it
    Pool pool = null;
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      pool = (Pool) session.byId(Pool.class).load(poolKey);
      if (pool == null) {
        return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("message", "Could not load the pool")).build();
      }
      // Force load
      pool.getDevices().size();
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    SimpleResponse s = new SimpleResponse("device", pool.getDevices());
    return Response.ok(s).build();
  }

  /** Create a Device. */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createDevice(Device device) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      Pool pool = (Pool) session.byId(Pool.class).getReference(poolKey);
      // Ensure the device does not think it already exists!
      device.deviceKey = -1;
      device.setPool(pool);
      pool.getDevices().add(device);
      session.getTransaction().commit();
    } catch (Exception e) {
      logger.error("Error creating device", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new SimpleResponse("message", "Error creating pool")).build();
    } finally {
      session.close();
    }
    return Response.ok(device).build();
  }

  /** Modify a Device. */
  @PUT
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifyDevice(@PathParam("id") int id, Device update) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    Device device = null;
    try {
      session.beginTransaction();
      device = (Device) session.byId(Device.class).load(id);
      if (device == null) {
        return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("message", "Could not load the device")).build();
      }
      if (device.pool.poolKey != poolKey) {
        return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("message", "Could not load the device")).build();
      }
      device.update(update);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok(device).build();
  }

  /** Delete a Device. */
  @DELETE
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteDevice(@PathParam("id") int id) {
    // Look up the pool and change it
    Session session = sessionFactory.openSession();
    Device device;
    try {
      session.beginTransaction();
      device = (Device) session.byId(Device.class).getReference(id);
      if (device.pool.poolKey != poolKey) {
        return Response.status(Status.NOT_FOUND).entity(new SimpleResponse("message", "Could not load the device")).build();
      }
      session.delete(device);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    SimpleResponse response = new SimpleResponse();
    response.put("status", "success");
    response.put("message", "Delete device " + device.deviceKey);
    return Response.ok(response).build();
  }
}
