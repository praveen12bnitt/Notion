package edu.mayo.qia.pacs.rest;

import javax.ws.rs.core.MediaType;

import edu.mayo.qia.pacs.Notion;

@Path("metrics")
@Component
public class MetricsEndpoint {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMetrics() {
    Notion.metrics
  }
}
