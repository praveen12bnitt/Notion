package edu.mayo.qia.pacs.rest;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.spi.resource.PerRequest;

@Scope("prototype")
@Component
@PerRequest
public class QueryEndpoint {
  static Logger logger = Logger.getLogger(QueryEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  public int poolKey;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response createQuery(@FormParam("file") InputStream spreadSheetInputStream, @FormParam("file") FormDataContentDisposition fileDetail) {

    // Use POI or SuperCSV to parse
    fileDetail.getFileName();

    return Response.ok().build();

  }
}
