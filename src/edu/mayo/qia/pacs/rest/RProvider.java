package edu.mayo.qia.pacs.rest;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Inject;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;

import edu.mayo.qia.pacs.components.Pool;

//@Provider
public class RProvider extends PerRequestTypeInjectableProvider<Context, Pool> {

  public RProvider(Type t) {
    super(t);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Injectable<Pool> getInjectable(ComponentContext ic, Context a) {
    // TODO Auto-generated method stub
    return new Injectable<Pool>() {
      @Override
      public Pool getValue() {
        return new Pool("Injected", "Injected from RProvider");
      }
    };
  }

}
