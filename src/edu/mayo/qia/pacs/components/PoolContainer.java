package edu.mayo.qia.pacs.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.PipelineStage;
import org.rsna.ctp.pipeline.Processor;
import org.rsna.ctp.stdstages.DicomAnonymizer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.ctp.Anonymizer;
import edu.mayo.qia.pacs.dicom.TagLoader;

/**
 * Manage a particular pool.
 * 
 * @author Daniel Blezek
 * 
 */
public class PoolContainer {
  static Logger logger = Logger.getLogger(PoolContainer.class);
  volatile Pool pool;
  File poolDirectory;
  File quarantinesDirectory;
  File scriptsDirectory;
  PipelineStage ctpAnonymizer = null;

  private JdbcTemplate template;
  private String sequenceName;

  public PoolContainer(Pool pool) {
    this.pool = pool;
    if (this.pool.poolKey <= 0) {
      throw new RuntimeException("PoolKey must be set!");
    }
    this.template = PACS.context.getBean("template", JdbcTemplate.class);
  }

  public void start() {
    logger.info("Starting up pool: " + pool);

    // Do we have a sequence for this pool?
    this.sequenceName = "UID" + pool.poolKey;
    boolean sequenceExists = false;
    try {
      template.queryForObject("VALUES SYSCS_UTIL.SYSCS_PEEK_AT_SEQUENCE('APP', ?)", Integer.class, this.sequenceName);
      sequenceExists = true;
    } catch (Exception e) {
      logger.error("Did not find sequence: " + this.sequenceName);
    }
    try {
      if (!sequenceExists) {
        template.update("create sequence " + sequenceName + " AS INT START WITH 1");
      }
    } catch (Exception e) {
      logger.error("Error creating sequence", e);
    }

    // See if the directory exists
    File poolBase = new File(PACS.directory, "ImageStorage");
    poolDirectory = new File(poolBase, Integer.toString(pool.poolKey));
    if (!poolDirectory.exists()) {
      poolDirectory.mkdirs();
    }
    quarantinesDirectory = new File(poolDirectory, "quarantines");
    quarantinesDirectory.mkdirs();
    scriptsDirectory = new File(poolDirectory, "scripts");
    scriptsDirectory.mkdirs();
    // Would start CTP here as well
    configureCTP();
  }

  public FileObject executePipeline(FileObject fileObject) throws Exception {
    if (pool.anonymize) {
      // Execute all the stages
      // 1. Prepare the Javascript anonymizer
      // 2. Execute the CTP anonymizer
      // 3. Execute the Javascript anonymizer

      // Load the tags, replace PatientName, PatientID and AccessionNumber
      DicomObject originalTags = TagLoader.loadTags(fileObject.getFile());

      FileObject stageOne = ((Processor) ctpAnonymizer).process(fileObject);
      fileObject = Anonymizer.process(this, stageOne, originalTags);
    }
    return fileObject;
  }

  public void configureCTP() {

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;
    try {
      docBuilder = docFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      logger.error("Failed to construct an XML builder", e);
      return;
    }

    // root elements
    Document doc = docBuilder.newDocument();
    Element element;
    element = doc.createElement("DicomAnonymizer");
    element.setAttribute("name", "DicomAnonymizer");
    element.setAttribute("root", new File(poolDirectory, "anonymizer").getAbsolutePath());
    File script = new File(scriptsDirectory, "anonymizer.script");

    ClassPathResource resource = new ClassPathResource("ctp/anonymizer.script");
    try {
      IOUtils.copy(resource.getInputStream(), new FileOutputStream(script));
    } catch (Exception e) {
      logger.error("Error copying the anonymizer script", e);
    }
    element.setAttribute("script", script.getAbsolutePath());
    ctpAnonymizer = new DicomAnonymizer(element);
    ctpAnonymizer.start();
  }

  public String getCTPConfig() throws Exception {
    File script = new File(scriptsDirectory, "anonymizer.script");
    return IOUtils.toString(new FileInputStream(script));
  }

  public void putCTPConfig(String text) throws Exception {
    File script = new File(scriptsDirectory, "anonymizer.script");
    FileOutputStream fos = new FileOutputStream(script);
    try {
      IOUtils.write(text, fos);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  public void stop() {
    logger.info("Shutting down pool: " + pool);
    // Stop all the stages
    ctpAnonymizer.shutdown();
  }

  public File getPoolDirectory() {
    return poolDirectory;
  }

  public void delete() {
    // Delete all the files associated with the container
    try {
      FileUtils.deleteDirectory(poolDirectory);
    } catch (IOException e) {
      logger.error("Failed to delete directory", e);
    }

  }

  public Pool getPool() {
    return pool;
  }

  public void update(Pool pool) {
    this.pool = pool;
  }

}
