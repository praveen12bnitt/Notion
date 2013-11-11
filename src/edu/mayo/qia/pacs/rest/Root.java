package edu.mayo.qia.pacs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.springframework.stereotype.Component;

@Path("/")
@Component
public class Root {
  @GET
  public String status() {
    return "ok";
  }
}
