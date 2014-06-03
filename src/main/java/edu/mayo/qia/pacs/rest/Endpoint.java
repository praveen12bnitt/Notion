package edu.mayo.qia.pacs.rest;

import javax.ws.rs.core.Context;

import com.sun.jersey.api.core.ResourceContext;

import edu.mayo.qia.pacs.Notion;

public class Endpoint {
  @Context
  ResourceContext resourceContext;

  <T> T getResource(Class<T> c) {
    T object = resourceContext.getResource(c);
    Notion.context.getAutowireCapableBeanFactory().autowireBean(object);
    return object;
  }
}
