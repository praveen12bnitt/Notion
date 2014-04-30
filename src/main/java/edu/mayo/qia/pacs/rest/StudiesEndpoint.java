package edu.mayo.qia.pacs.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.PerRequest;

import edu.mayo.qia.pacs.components.Instance;
import edu.mayo.qia.pacs.components.PoolManager;
import edu.mayo.qia.pacs.components.Series;
import edu.mayo.qia.pacs.components.Study;

@Scope("prototype")
@Component
@PerRequest
public class StudiesEndpoint {
  static Logger logger = Logger.getLogger(ScriptEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  @Autowired
  PoolManager poolManager;

  public int poolKey;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchStudies(@Context UriInfo uriInfo, JSONObject qParams) throws Exception {
    final Set<String> columns = new HashSet<String>(Arrays.asList(new String[] { "PatientID", "PatientName", "AccessionNumber", "StudyDescription" }));
    final Set<String> directions = new HashSet<String>();
    directions.add("ASC");
    directions.add("DESC");
    JSONObject json = new JSONObject();
    json.put("Result", "OK");
    // Build the query
    final JSONArray records = new JSONArray();
    json.put("Records", records);
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

    logger.info("query parameters\n" + queryParameters);

    logger.info("query object\n" + qParams);

    StringBuilder query = new StringBuilder("select * from STUDY where PoolKey = ?");
    ArrayList<Object> parameters = new ArrayList<Object>();
    parameters.add(poolKey);

    StringBuilder where = new StringBuilder(" ");

    for (String column : columns) {
      if (qParams.has(column)) {
        where.append(" and " + column + " like ? ");
        parameters.add("%" + qParams.getString(column) + "%");
      }
    }

    if (qParams.has("jtSorting")) {
      query.append(" ORDER BY ");

      for (String clause : qParams.getString("jtSorting").split(",")) {
        String[] p = clause.split("\\s+");
        if (columns.contains(p[0])) {
          where.append(p[0]);
        }
        if (directions.contains(p[1])) {
          where.append(" " + p[1]);
        }
      }
    }

    // do our count here:
    Integer count = template.queryForObject("select count(*) from STUDY where PoolKey = ? " + where, parameters.toArray(), Integer.class);
    // Also need to return the total number of records
    json.put("TotalRecordCount", count);
    query.append(where);
    // jtStartIndex: Start index of records for current page.
    // jtPageSize: Count of maximum expected records.
    if (qParams.has("jtStartIndex") && qParams.has("jtPageSize")) {
      query.append(" OFFSET ? ROWS ");
      parameters.add(qParams.optInt("jtStartIndex", 0));
      query.append(" FETCH NEXT ? ROWS ONLY ");
      parameters.add(qParams.optInt("jtPageSize", 50));
    }
    template.query(query.toString(), parameters.toArray(), new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        JSONObject row = new JSONObject();
        try {
          for (String column : columns) {
            row.put(column, rs.getString(column));
          }
          for (String column : new String[] { "StudyKey" }) {
            row.put(column, rs.getInt(column));
          }
        } catch (JSONException e) {
          logger.error("Error setting field", e);
        }
        records.put(row);
      }
    });

    return Response.ok(json).build();
  }

  // Get as a ZIP file
  @GET
  @Path("/{id: [1-9][0-9]*}/zip")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getZip(@PathParam("id") int id) throws Exception {
    Query query;
    final String regex = "[^0-9a-zA-Z_-]";
    Session session = sessionFactory.getCurrentSession();
    if (!session.getTransaction().isActive()) {
      session.getTransaction().begin();
    }
    query = session.createQuery("from Study where PoolKey = :poolkey and StudyKey = :id");
    query.setInteger("poolkey", poolKey);
    query.setInteger("id", id);
    final Study study = (Study) query.uniqueResult();
    if (study == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        ZipOutputStream zip = new ZipOutputStream(output);
        File poolRootDir = poolManager.getContainer(poolKey).getPoolDirectory();
        String path = study.PatientName.replaceAll(regex, "_");
        // zip.putNextEntry(new ZipEntry(path));

        String sub = study.StudyID == null ? "" : study.StudyID.replaceAll(regex, "_") + "-";
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        path = path + "/" + sub + format.format(study.StudyDate);
        // zip.putNextEntry(new ZipEntry(path));
        for (Series series : study.series) {
          String seriesPath = path + "/" + series.SeriesDescription.replaceAll(regex, "_");
          // zip.putNextEntry(new ZipEntry(seriesPath));
          for (Instance instance : series.instances) {
            String instancePath = seriesPath + "/" + instance.SOPInstanceUID + ".dcm";
            zip.putNextEntry(new ZipEntry(instancePath));
            File f = new File(poolRootDir, instance.FilePath);
            // Read into the zip file
            FileInputStream in = new FileInputStream(f);
            int len;
            while ((len = in.read(buffer)) > 0) {
              zip.write(buffer, 0, len);
            }
            in.close();
            zip.closeEntry();
          }
        }
        zip.closeEntry();
        zip.close();
      }
    };
    String fn = study.PatientName.replaceAll(regex, "_") + ".zip";
    return Response.ok(stream).header("content-disposition", "attachment; filename = " + fn).build();
  }

  /** Delete a Study. */
  @DELETE
  @Path("/{id: [1-9][0-9]*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteScript(@PathParam("id") int id) {
    if (poolManager.getContainer(poolKey) != null) {
      poolManager.getContainer(poolKey).deleteStudy(id);
    }
    SimpleResponse response = new SimpleResponse();
    response.put("status", "success");
    response.put("message", "Delete study " + id);
    return Response.ok(response).build();
  }

  @POST
  @Path("/delete")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteRecord(MultivaluedMap<String, String> formParams, @FormParam("StudyKey") int studyKey) {
    logger.info("Form Parameters: " + formParams);
    poolManager.getContainer(poolKey).deleteStudy(studyKey);
    return Response.ok(new SimpleResponse("Result", "OK")).build();
  }
}
