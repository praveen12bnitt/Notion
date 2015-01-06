package edu.mayo.qia.pacs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.Notion;

@Path("/metrics")
@Component
@Scope("singleton")
public class MetricsEndpoint {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMetrics() {
    return Response.ok(Notion.metrics).build();
  }
}
