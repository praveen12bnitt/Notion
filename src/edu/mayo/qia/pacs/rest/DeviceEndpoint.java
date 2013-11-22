package edu.mayo.qia.pacs.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

  /** List all the pools */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Device> listDevices() {
    // Look up the pool and change it
    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    Pool pool = (Pool) session.byId(Pool.class).getReference(poolKey);
    // Force load
    pool.getDevices().size();
    session.getTransaction().commit();
    return pool.getDevices();
  }

  /** Create a Device. */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createDevice(Device device) {
    // Look up the pool and change it
    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    Pool pool = (Pool) session.byId(Pool.class).getReference(poolKey);
    device.setPool(pool);
    pool.getDevices().add(device);
    session.getTransaction().commit();
    return Response.ok(device).build();
  }

  /** Delete a Device. */
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteDevice(Device device) {
    // Look up the pool and change it
    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    device = (Device) session.byId(Device.class).getReference(device.deviceKey);
    session.delete(device);
    session.getTransaction().commit();
    Map<String, String> response = new HashMap<String, String>();
    response.put("status", "success");
    response.put("message", "Delete device " + device.deviceKey);
    return Response.ok().build();
  }

}
