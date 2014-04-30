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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.core.ResourceContext;

import edu.mayo.qia.pacs.components.Connector;
import edu.mayo.qia.pacs.components.PoolManager;

@Component
@Path("/connector")
@Scope("singleton")
public class ConnectorEndpoint {
  static Logger logger = Logger.getLogger(ConnectorEndpoint.class);

  @Autowired
  JdbcTemplate template;

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  PoolManager poolManager;

  @Autowired
  ObjectMapper objectMapper;

  @Context
  ResourceContext resourceContext;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConnector() {
    List<Connector> result = new ArrayList<Connector>();
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      result = session.createCriteria(Connector.class).list();
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    SimpleResponse s = new SimpleResponse("connector", result);
    return Response.ok(s).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createConnector(Connector connector) {
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      session.save(connector);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok(connector).build();
  }

  @PUT
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateConnector(@PathParam("id") int id, Connector connector) {
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      session.update(connector);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok(connector).build();
  }

  @DELETE
  @Path("/{id: [1-9][0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteConnector(Connector connector) {
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      session.delete(connector);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    return Response.ok().build();
  }
}
