package edu.mayo.qia.pacs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.Singleton;

@Path("/")
@Component
@Singleton
public class Root {
  @GET
  public String status() {
    return "ok";
  }
}
