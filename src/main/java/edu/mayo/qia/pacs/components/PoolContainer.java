package edu.mayo.qia.pacs.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dcm4che.dict.Tags;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.PipelineStage;
import org.rsna.ctp.pipeline.Processor;
import org.rsna.ctp.stdstages.DicomAnonymizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.Files;

import edu.mayo.qia.pacs.Notion;
import edu.mayo.qia.pacs.NotionConfiguration;
import edu.mayo.qia.pacs.ctp.Anonymizer;
import edu.mayo.qia.pacs.dicom.DICOMReceiver.AssociationInfo;
import edu.mayo.qia.pacs.dicom.TagLoader;

/**
 * Manage a particular pool.
 * 
 * @author Daniel Blezek
 * 
 */
@Component
@Scope("prototype")
public class PoolContainer {
  static Logger logger = Logger.getLogger(PoolContainer.class);

  public static final int[] SortedFilename = { Tags.AccessionNumber, Tags.StudyInstanceUID, Tags.SeriesInstanceUID, Tags.SOPInstanceUID };

  volatile Pool pool;
  File poolDirectory;
  File quarantinesDirectory;
  File scriptsDirectory;
  File incomingDirectory;
  File imageDirectory;
  PipelineStage ctpAnonymizer = null;

  @Autowired
  private JdbcTemplate template;

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  PoolManager poolManager;

  @Autowired
  NotionConfiguration configuration;

  private String sequenceName;

  public PoolContainer() {
  }

  public void start(Pool pool) {
    this.pool = pool;
    if (this.pool.poolKey <= 0) {
      throw new RuntimeException("PoolKey must be set!");
    }
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
    File poolBase = new File(configuration.notion.imageDirectory, "ImageStorage");
    poolDirectory = new File(poolBase, Integer.toString(pool.poolKey));
    if (!poolDirectory.exists()) {
      poolDirectory.mkdirs();
    }
    quarantinesDirectory = new File(poolDirectory, "quarantines");
    quarantinesDirectory.mkdirs();
    incomingDirectory = new File(poolDirectory, "incoming");
    incomingDirectory.mkdirs();
    incomingDirectory = new File(poolDirectory, "incoming");
    incomingDirectory.mkdirs();
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

  public void deleteStudy(String studyInstanceUID) {
    Integer studyKey = template.queryForObject("select StudyKey from STUDY where PoolKey = ? and StudyInstanceUID = ?", new Object[] { pool.poolKey, studyInstanceUID }, Integer.class);
    if (studyKey != null) {
      deleteStudy(studyKey);
    }
  }

  public boolean deleteStudy(final int studyKey) {
    Integer count = template.queryForObject("select count(*) from STUDY where PoolKey = ? and StudyKey = ?", new Object[] { pool.poolKey, studyKey }, Integer.class);
    if (count.intValue() != 1) {
      return false;
    }

    // Delete the study, series and instances
    synchronized (this) {
      final File deleteDirectory = new File(poolDirectory, "deleted");
      deleteDirectory.mkdirs();
      final Set<File> directories = new HashSet<File>();

      // Collect all the files to delete
      List<String> filePaths = template.queryForList("select FilePath from INSTANCE, SERIES, STUDY where INSTANCE.SeriesKey = SERIES.SeriesKey and SERIES.StudyKey = STUDY.StudyKey and STUDY.PoolKey = ? and STUDY.StudyKey = ?", new Object[] { pool.poolKey,
          studyKey }, String.class);
      // Delete, should cascade!
      template.update("delete from STUDY where PoolKey = ? and StudyKey = ?", pool.poolKey, studyKey);

      logger.error("\n\n\t=====\n\n\tDeleting STUDY " + studyKey + " from POOL " + pool.poolKey + "\n\n\t=====\n\n");

      for (String filePath : filePaths) {
        File file = new File(poolDirectory, filePath);
        directories.add(file.getParentFile());
        try {
          FileUtils.moveFile(file, new File(deleteDirectory, UUID.randomUUID().toString()));
        } catch (IOException e) {
          logger.error("Failed to move file", e);
        }
      }

      // Delete the deleted directory
      for (File file : deleteDirectory.listFiles()) {
        if (file.isFile()) {
          file.delete();
        }
      }
      for (File dir : directories) {
        dir.delete();
      }
      return true;
    }
  }

  /**
   * Move a file from a different pool
   * 
   * @throws Exception
   */
  public void importFromPool(File incoming) throws Exception {
    File importFile = new File(incomingDirectory, UUID.randomUUID().toString());
    FileUtils.copyFile(incoming, importFile);
    process(importFile);
  }

  String noNulls(String in) {
    return in != null ? in : "";
  }

  public void process(File incoming) throws Exception, IOException {
    synchronized (this) {
      // Handle one per container
      // Have the container process!
      DicomObject originalTags = TagLoader.loadTags(incoming);

      org.rsna.ctp.objects.DicomObject fileObject = new org.rsna.ctp.objects.DicomObject(incoming);
      FileObject outObject = this.executePipeline(fileObject);
      File inFile = outObject.getFile();
      File originalFile = incoming;

      // Index the file
      DicomObject tags = TagLoader.loadTags(inFile);
      logger.info("Saving file for " + tags.getString(Tag.PatientName));

      File relativePath = constructRelativeFileName(tags);

      File outFile = new File(this.poolDirectory, relativePath.getPath());

      if (!inFile.exists()) {
        logger.error("Input file " + inFile + " does not exist");
        throw new Exception("Input file " + inFile + " does not exist");
      }

      logger.debug("Final path: " + outFile);
      outFile.getParentFile().mkdirs();

      // Insert
      Session session = sessionFactory.openSession();
      session.beginTransaction();

      try {

        Query query;
        query = session.createQuery("from Study where PoolKey = :poolkey and StudyInstanceUID = :suid");
        query.setInteger("poolkey", pool.poolKey);
        query.setString("suid", tags.getString(Tag.StudyInstanceUID));
        Study study = (Study) query.uniqueResult();
        if (study == null) {
          study = new Study(tags);
          study.pool = pool;
        } else {
          study.update(tags);
        }
        session.saveOrUpdate(study);

        // Find the Instance
        query = session.createQuery("from Series where StudyKey = :studykey and SeriesInstanceUID = :suid");
        query.setInteger("studykey", study.StudyKey);
        query.setString("suid", tags.getString(Tag.SeriesInstanceUID));
        Series series = (Series) query.uniqueResult();
        if (series == null) {
          series = new Series(tags);
          series.study = study;
        } else {
          series.update(tags);
        }
        session.saveOrUpdate(series);

        // Find the Instance
        query = session.createQuery("from Instance where SeriesKey = :serieskey and SOPInstanceUID = :suid").setInteger("serieskey", series.SeriesKey);
        query.setString("suid", tags.getString(Tag.SOPInstanceUID));
        Instance instance = (Instance) query.uniqueResult();
        if (instance == null) {
          instance = new Instance(tags, relativePath.getPath());
          instance.series = series;
        } else {
          instance.update(tags);
        }
        session.saveOrUpdate(instance);

        // Copy the file, remove later
        Files.copy(inFile, outFile);
        logger.info("Moved file " + inFile + " to " + outFile);

        // Delete the input file, it is not needed any more
        if (inFile.exists()) {
          inFile.delete();
        }
        if (originalFile.exists()) {
          originalFile.delete();
        }
        session.getTransaction().commit();

        if (pool.anonymize) {

          // Save the association info for later
          template
              .update(
                  "insert into ANONYMIZATIONMAPTEMP ( PoolKey, OriginalPatientName, AnonymizedPatientName, OriginalPatientID, AnonymizedPatientID, OriginalAccessionNumber, AnonymizedAccessionNumber, OriginalPatientBirthDate, AnonymizedPatientBirthDate ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? )",
                  pool.poolKey, noNulls(originalTags.getString(Tag.PatientName)), noNulls(tags.getString(Tag.PatientName)), noNulls(originalTags.getString(Tag.PatientID)), noNulls(tags.getString(Tag.PatientID)),
                  noNulls(originalTags.getString(Tag.AccessionNumber)), noNulls(tags.getString(Tag.AccessionNumber)), noNulls(originalTags.getString(Tag.PatientBirthDate)), noNulls(tags.getString(Tag.PatientBirthDate)));
        }
      } catch (Exception e) {
        logger.error("Caught exception", e);
      } finally {
        session.close();
      }
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
    stop();
    // First start by deleting all the studies
    template.query("select StudyKey from STUDY where PoolKey = ?", new Object[] { pool.poolKey }, new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        deleteStudy(rs.getInt("StudyKey"));
      }
    });

    // Will cascade to all other tables
    template.update("delete from POOL where PoolKey = ?", pool.poolKey);

    // Delete all the files associated with the container
    try {
      FileUtils.deleteDirectory(poolDirectory);
    } catch (IOException e) {
      logger.error("Failed to delete directory", e);
    }
    // Remove this from the manager
    poolManager.remove(this);
  }

  public Pool getPool() {
    return pool;
  }

  public void update(Pool pool) {
    this.pool = pool;
  }

  static File constructRelativeFileName(DicomObject dataset) {
    File relativePath = new File("sorted");
    for (int tag : SortedFilename) {
      String t = dataset.getString(tag);
      if (t == null) {
        t = "UNKNOWN";
      }
      relativePath = new File(relativePath, t);
    }
    // Always store with .dcm at the end
    relativePath = new File(relativePath.getParentFile(), relativePath.getName() + ".dcm");
    return relativePath;
  }

  public boolean moveStudyTo(String studyInstanceUID, final PoolContainer destination) {
    final AtomicBoolean successful = new AtomicBoolean(true);
    template.query("select INSTANCE.FilePath from INSTANCE, STUDY, SERIES, POOL where STUDY.PoolKey = ? AND INSTANCE.SeriesKey = SERIES.SeriesKey and SERIES.StudyKey = STUDY.StudyKey and STUDY.StudyInstanceUID = ?", new Object[] { this.pool.poolKey,
        studyInstanceUID }, new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        File f = new File(getPoolDirectory(), rs.getString("FilePath"));
        File tempDir = new File(getPoolDirectory(), "incoming");
        tempDir.mkdirs();
        File tmpFile = new File(tempDir, UUID.randomUUID().toString() + ".dcm");
        try {
          Files.copy(f, tmpFile);
          destination.process(tmpFile);
        } catch (Exception e) {
          successful.set(false);
          logger.error("Error processing file: " + f + " into pool " + pool, e);
        }
      }
    });
    destination.processAnonymizationMap();
    return successful.get();
  }

  public void processAnonymizationMap() {
    Notion.context.getBean("executor", ExecutorService.class).execute(new Runnable() {

      @Override
      public void run() {
        synchronized (this) {
          template.query("select distinct OriginalPatientName, AnonymizedPatientName, OriginalPatientID, AnonymizedPatientID, OriginalAccessionNumber, AnonymizedAccessionNumber, OriginalPatientBirthDate, AnonymizedPatientBirthDate"
              + " from ANONYMIZATIONMAPTEMP where PoolKey = ?", new Object[] { pool.poolKey }, new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {
              String opn, apn, opi, api, oan, aan, opbd, apdb;
              opn = rs.getString(1);
              apn = rs.getString(2);
              opi = rs.getString(3);
              api = rs.getString(4);
              oan = rs.getString(5);
              aan = rs.getString(6);
              opbd = rs.getString(7);
              apdb = rs.getString(8);

              int count = template
                  .queryForObject(
                      "select count(*) from ANONYMIZATIONMAP where PoolKey = ? and OriginalPatientName = ? and AnonymizedPatientName = ? and OriginalPatientID = ? and AnonymizedPatientID = ? and OriginalAccessionNumber = ? and AnonymizedAccessionNumber = ? and OriginalPatientBirthDate = ? and AnonymizedPatientBirthDate = ?",
                      new Object[] { pool.poolKey, opn, apn, opi, api, oan, aan, opbd, apdb }, Integer.class);

              if (count == 0) {
                template
                    .update(
                        "insert into ANONYMIZATIONMAP ( PoolKey, OriginalPatientName, AnonymizedPatientName, OriginalPatientID, AnonymizedPatientID, OriginalAccessionNumber, AnonymizedAccessionNumber, OriginalPatientBirthDate, AnonymizedPatientBirthDate ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? )",
                        new Object[] { pool.poolKey, opn, apn, opi, api, oan, aan, opbd, apdb });
              } else {
                template
                    .update(
                        "update ANONYMIZATIONMAP set UpdatedTimestamp = current_timestamp where PoolKey = ? and OriginalPatientName = ? and AnonymizedPatientName = ? and OriginalPatientID = ? and AnonymizedPatientID = ? and OriginalAccessionNumber = ? and AnonymizedAccessionNumber = ? and OriginalPatientBirthDate = ? and AnonymizedPatientBirthDate = ?",
                        new Object[] { pool.poolKey, opn, apn, opi, api, oan, aan, opbd, apdb });

              }
              template
                  .update(
                      "delete from ANONYMIZATIONMAPTEMP where PoolKey = ? and OriginalPatientName = ? and AnonymizedPatientName = ? and OriginalPatientID = ? and AnonymizedPatientID = ? and OriginalAccessionNumber = ? and AnonymizedAccessionNumber = ? and OriginalPatientBirthDate = ? and AnonymizedPatientBirthDate = ?",
                      new Object[] { pool.poolKey, opn, apn, opi, api, oan, aan, opbd, apdb });

            }
          });
        }
      }
    });
  }
}