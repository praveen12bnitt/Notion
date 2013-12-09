package edu.mayo.qia.pacs.ctp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dcm4che.dict.Tags;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.util.TagUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.util.DigestUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.components.Pool;
import edu.mayo.qia.pacs.components.PoolContainer;
import edu.mayo.qia.pacs.dicom.TagLoader;

/** Helper class containing some functions to use for the Anonymizer */
public class Anonymizer {
  static Logger logger = Logger.getLogger(Anonymizer.class);
  private Pool pool;
  JdbcTemplate template;

  public Anonymizer(Pool pool) {
    this.pool = pool;
    template = PACS.context.getBean("template", JdbcTemplate.class);
  }

  public ScriptableObject setBindings(ScriptableObject scope, DicomObject tags) {
    ScriptableObject.putProperty(scope, "pool", pool);
    ScriptableObject.putProperty(scope, "anon", this);
    ScriptableObject.putProperty(scope, "anonymizer", this);
    NativeObject tagObject = new NativeObject();
    Iterator<DicomElement> iterator = tags.datasetIterator();
    while (iterator.hasNext()) {
      DicomElement element = iterator.next();
      String tagName = ElementDictionary.getDictionary().nameOf(element.tag());
      tagName = tagName.replaceAll("[ ']+", "");
      tagObject.defineProperty(tagName, tags.getString(element.tag()), NativeObject.READONLY);
      // logger.info("Setting: " + tagName + ": " +
      // tags.getString(element.tag()));
    }
    ScriptableObject.putProperty(scope, "tags", tagObject);
    return scope;
  }

  public String hash(String value) {
    return hash(value, value.length());
  }

  public String lookup(String type, String name) {
    final String[] out = new String[] { null };
    template.query("select Value from LOOKUP where PoolKey = ? and Type = ? and Name = ?", new Object[] { pool.poolKey, type, name }, new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        out[0] = rs.getString("Value");
      }
    });
    return out[0];
  }

  public String hash(String value, int length) {
    String result = value;
    try {
      result = DigestUtil.getUSMD5(value);
    } catch (Exception e) {
      logger.error("Failed to compute MD5 hash", e);
    }
    return result.substring(0, length);
  }

  public static FileObject process(PoolContainer poolContainer, FileObject fileObject, File original) {
    JdbcTemplate template = PACS.context.getBean("template", JdbcTemplate.class);

    try {
      // Load the tags, replace PatientName, PatientID and AccessionNumber
      DicomInputStream dis = new DicomInputStream(fileObject.getFile());
      final DicomObject dcm = dis.readDicomObject();
      dis.close();
      DicomObject originalTags = TagLoader.loadTags(original);

      Anonymizer function = new Anonymizer(poolContainer.getPool());

      final Context context = Context.enter();
      try {
        final ScriptableObject scope = function.setBindings(context.initStandardObjects(), originalTags);

        // Run through all the stored bindings
        template.query("select Tag, Script from SCRIPT where PoolKey = ?", new Object[] { poolContainer.getPool().poolKey }, new RowCallbackHandler() {

          @Override
          public void processRow(ResultSet rs) throws SQLException {
            String tag = rs.getString("Tag");
            int tagValue = Tag.toTag(tag);
            String script = rs.getString("Script");
            logger.info("Processing tag: " + tag + " with script: " + script);
            Object result = dcm.getString(tagValue);
            try {
              result = context.evaluateString(scope, script, "inline", 1, null);
            } catch (Exception e) {
              logger.error("Failed to process the script correctly: " + script, e);
            }
            if (result instanceof String) {
              dcm.putString(tagValue, dcm.get(tagValue).vr(), (String) result);
            } else {
              logger.error("Expected a string back from script, but instead got: " + result.toString());
            }

          }
        });

      } finally {
        Context.exit();
      }
      File output = new File(fileObject.getFile().getParentFile(), UUID.randomUUID().toString());
      FileOutputStream fos = new FileOutputStream(output);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      DicomOutputStream dos = new DicomOutputStream(bos);
      dos.writeDicomFile(dcm);
      dos.close();

      fileObject.getFile().delete();
      FileUtils.moveFile(output, fileObject.getFile());
    } catch (Exception e) {
      logger.error("Error changing patient info", e);
    }
    return fileObject;
  }
}
