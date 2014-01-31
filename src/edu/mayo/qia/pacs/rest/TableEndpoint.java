package edu.mayo.qia.pacs.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.components.PoolManager;

@Scope("prototype")
@Component
public class TableEndpoint {
  static Logger logger = Logger.getLogger(LookupEndpoint.class);

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  @Autowired
  PoolManager poolManager;

  public int poolKey;

  public JSONObject get(MultivaluedMap<String, String> queryParameters, String table, String extraWhere, String[] columnArray, final String keyColumn) throws Exception {
    final Set<String> columns = new HashSet<String>(Arrays.asList(columnArray));
    final Set<String> directions = new HashSet<String>();
    directions.add("ASC");
    directions.add("DESC");
    JSONObject json = new JSONObject();
    json.put("Result", "OK");
    // Build the query
    final JSONArray records = new JSONArray();
    json.put("Records", records);

    StringBuilder query = new StringBuilder("select * from " + table + " where PoolKey = ? " + extraWhere);
    ArrayList<Object> parameters = new ArrayList<Object>();
    parameters.add(poolKey);

    if (queryParameters.containsKey("jtSorting")) {
      query.append(" ORDER BY ");

      for (String clause : queryParameters.getFirst("jtSorting").split(",")) {
        String[] p = clause.split("\\s+");
        if (columns.contains(p[0])) {
          query.append(p[0]);
        }
        if (directions.contains(p[1])) {
          query.append(" " + p[1]);
        }
      }
    }

    // jtStartIndex: Start index of records for current page.
    // jtPageSize: Count of maximum expected records.
    if (queryParameters.containsKey("jtStartIndex") && queryParameters.containsKey("jtPageSize")) {
      query.append(" OFFSET ? ROWS ");
      parameters.add(queryParameters.getFirst("jtStartIndex"));
      query.append(" FETCH NEXT ? ROWS ONLY ");
      parameters.add(queryParameters.getFirst("jtPageSize"));

      // Also need to return the total number of records
      json.put("TotalRecordCount", template.queryForObject("select count(*) from " + table + " where PoolKey = ? " + extraWhere, new Object[] { poolKey }, Integer.class));
    }

    template.query(query.toString(), parameters.toArray(), new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        JSONObject row = new JSONObject();
        try {
          for (String column : columns) {
            row.put(column, rs.getString(column));
          }
          row.put(keyColumn, rs.getInt(keyColumn));
        } catch (JSONException e) {
          logger.error("Error setting field", e);
        }
        records.put(row);
      }
    });

    return json;
  }

}
