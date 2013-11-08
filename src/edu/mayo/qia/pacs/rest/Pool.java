package edu.mayo.qia.pacs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Component
@Path("/pool")
public class Pool {

  @Autowired
  JdbcTemplate template;

  /** List all the pools */
  @GET
  public String listPools() {
    if (template == null) {
      return "epic fail";
    } else {
      return "{ template : " + template.toString() + "}";
    }
  }

}
