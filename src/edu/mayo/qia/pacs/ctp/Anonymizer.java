package edu.mayo.qia.pacs.ctp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
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
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.rsna.ctp.objects.FileObject;
import org.springframework.jdbc.core.JdbcTemplate;
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

  public Bindings setBindings(Bindings bindings, DicomObject tags) {
    bindings.put("pool", pool);
    bindings.put("anon", this);
    bindings.put("anonymizer", this);

    Iterator<DicomElement> iterator = tags.datasetIterator();
    while (iterator.hasNext()) {
      DicomElement element = iterator.next();
      bindings.put(Tags.toString(element.tag()), tags.getString(element.tag()));
    }
    return bindings;
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

      ScriptEngineManager manager = new ScriptEngineManager();
      final ScriptEngine engine = manager.getEngineByName("JavaScript");
      final Bindings bindings = function.setBindings(engine.createBindings(), originalTags);

      // Run through all the stored bindings
      template.query("select Tag, Script from SCRIPT where PoolKey = ?", new Object[] { poolContainer.getPool().poolKey }, new RowCallbackHandler() {

        @Override
        public void processRow(ResultSet rs) throws SQLException {
          String tag = rs.getString("Tag");
          int tagValue = Tag.toTag(tag);
          String script = rs.getString("Script");
          logger.info("Processing tag: " + tag);
          Object result = dcm.getString(tagValue);
          try {
            result = engine.eval(script, bindings);
          } catch (ScriptException e) {
            logger.error("Error processing script", e);
          }
          if (result instanceof String) {
            dcm.putString(tagValue, dcm.get(tagValue).vr(), (String) result);
          } else {
            logger.error("Expected a string back from script");
          }

        }
      });

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
