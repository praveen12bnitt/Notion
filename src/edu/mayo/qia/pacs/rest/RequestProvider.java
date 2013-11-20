package edu.mayo.qia.pacs.rest;

import java.lang.reflect.Type;

import javax.annotation.Resource;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.factory.InjectableProviderFactory;
import com.sun.jersey.spi.inject.*;

import edu.mayo.qia.pacs.components.Pool;

//@Provider
public class RequestProvider implements InjectableProvider<Context, Type> {
  static Logger logger = Logger.getLogger(RequestProvider.class);

  public RequestProvider() {
    logger.error("Made me one of those");
  }

  @Override
  public Injectable getInjectable(ComponentContext ic, Context a, Type c) {
    logger.error("Processing type: " + c);
    if (Integer.class == c) {
      return new Injectable<Integer>() {
        @Override
        public Integer getValue() {
          return new Integer(99);
        }
      };
    }
    if (Pool.class == c) {
      return new Injectable<Pool>() {
        @Override
        public Pool getValue() {
          return new Pool("Foo", "Constructed", "foo");
        }
      };

    }
    return null;

  }

  @Override
  public ComponentScope getScope() {
    // TODO Auto-generated method stub
    return ComponentScope.PerRequest;
  }

}