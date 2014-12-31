package edu.mayo.qia.pacs.rest;

import javax.ws.rs.core.Context;

import com.sun.jersey.api.core.ResourceContext;

import edu.mayo.qia.pacs.Notion;

/**
 * A Notion REST endpoint.
 * 
 * This class exists simply to autowire REST endpoints. A new endpoint must be
 * registered with the Spring container in NotionApplication as:
 * <code>environment.jersey().register(context.getBean(MetricsEndpoint.class));</code>
 * 
 * @author Daniel Blezek
 *
 */
public class Endpoint {
  @Context
  ResourceContext resourceContext;

  <T> T getResource(Class<T> c) {
    T object = resourceContext.getResource(c);
    Notion.context.getAutowireCapableBeanFactory().autowireBean(object);
    return object;
  }
}
