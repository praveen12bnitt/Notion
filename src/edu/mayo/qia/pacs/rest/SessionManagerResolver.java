package edu.mayo.qia.pacs.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;

import edu.mayo.qia.pacs.SessionManager;

class SessionManagerResolver extends AbstractHttpContextInjectable<SessionManager> implements InjectableProvider<Context, SessionManager> {

  public SessionManagerResolver() {
    super();
    // TODO Auto-generated constructor stub
  }

  public Injectable<SessionManager> getInjectable(ComponentContext ic, Context a) {
    // TODO Auto-generated method stub
    return null;
  }

  public SessionManager getContext(Class<?> type) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SessionManager getValue(HttpContext c) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable getInjectable(ComponentContext ic, Context a, SessionManager c) {
    // TODO Auto-generated method stub
    return null;
  }

}