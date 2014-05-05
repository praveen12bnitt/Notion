package edu.mayo.qia.pacs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.Notion;

@Path("/")
@Component
public class Root {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response status() {
    SimpleResponse response = new SimpleResponse();
    response.put("status", "ok");
    response.put("version", Notion.version);
    return Response.ok(response).build();
  }
}
