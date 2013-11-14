package edu.mayo.qia.pacs.rest;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.inject.Inject;

import edu.mayo.qia.pacs.SessionManager;
import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.components.Pool;

// Create a new one every time
@Component
@Scope("prototype")
public class DeviceEndpoint {

  @Context
  SessionManager sessionManager;

  @Autowired
  JdbcTemplate template;

  @Autowired
  SessionFactory sessionFactory;

  public Pool pool;

  /** List all the pools */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Device> listDevices() {

    Session session = sessionFactory.openSession();
    Hibernate.initialize(pool);
    Set<Device> devices = pool.getDevices();
    session.close();
    return devices;
  }
}
