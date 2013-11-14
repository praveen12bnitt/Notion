package edu.mayo.qia.pacs.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;

import edu.mayo.qia.pacs.SessionManager;

@Provider
class SessionManagerResolver extends PerRequestTypeInjectableProvider<Context, SessionManager> {

  public SessionManagerResolver() {
    super(SessionManager.class);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Injectable<SessionManager> getInjectable(ComponentContext ic, Context a) {
    // TODO Auto-generated method stub
    return null;
  }
}