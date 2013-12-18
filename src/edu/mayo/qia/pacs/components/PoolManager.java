package edu.mayo.qia.pacs.components;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rsna.ctp.objects.FileObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.google.common.io.Files;

import edu.mayo.qia.pacs.dicom.TagLoader;
import edu.mayo.qia.pacs.message.ProcessIncomingInstance;

/**
 * Manages the pools, starting them up, shutdown, etc.
 * 
 * @author Daniel Blezek
 * 
 */
@Component
public class PoolManager {
  static Logger logger = Logger.getLogger(PoolManager.class);
  public static final String SortedFilenameFormat = "AccessionNumber/StudyInstanceUID/SeriesInstanceUID/SOPInstanceUID";

  ConcurrentMap<String, Integer> instanceUIDs = new ConcurrentHashMap<String, Integer>();
  ConcurrentMap<String, Integer> seriesUIDs = new ConcurrentHashMap<String, Integer>();
  ConcurrentMap<String, Integer> studyUIDs = new ConcurrentHashMap<String, Integer>();
  ScheduledFuture<?> future;
  ConcurrentMap<String, PoolContainer> poolContainers = new ConcurrentHashMap<String, PoolContainer>();

  @Autowired
  JdbcTemplate template;

  @Autowired
  TaskScheduler taskScheduler;

  @Autowired
  SessionFactory sessionFactory;

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void startPools() {

    Session session = sessionFactory.getCurrentSession();
    session.beginTransaction();
    for (Pool pool : (List<Pool>) session.createQuery("from Pool").list()) {
      newPool(pool);
    }
    session.getTransaction().commit();

    // Schedule cleanup
    future = taskScheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        instanceUIDs.clear();
        seriesUIDs.clear();
        studyUIDs.clear();
      }
    }, 5000);
  }

  public void newPool(Pool pool) {
    PoolContainer poolContainer = new PoolContainer(pool);
    poolContainers.put(pool.applicationEntityTitle, poolContainer);
    logger.info("Starting pool: " + pool);
    poolContainer.start();

  }

  @PreDestroy
  public void stopPools() {
    for (PoolContainer poolContainer : poolContainers.values()) {
      poolContainer.stop();
    }
    future.cancel(true);
  }

  public PoolContainer getContainer(String aet) {
    return poolContainers.get(aet);
  }

  public PoolContainer getContainer(int poolKey) {
    for (PoolContainer container : poolContainers.values()) {
      if (container.pool.poolKey == poolKey) {
        return container;
      }
    }
    return null;
  }

  public void deletePool(Pool pool) {
    PoolContainer container = getContainer(pool.applicationEntityTitle);
    container.stop();
    poolContainers.remove(pool.applicationEntityTitle, container);
    container.delete();
  }

  /**
   * Handle a request to create an image, NB: must only handle one request at a
   * time.
   */
  public void handleMessage(ProcessIncomingInstance request) throws Exception {

    // Pull out the filename
    PoolContainer container = poolContainers.get(request.association.getCalledAET());
    if (container == null) {
      // Now what?
    }

    // Handle one per container
    synchronized (container) {
      // Have the container process!
      org.rsna.ctp.objects.DicomObject fileObject = new org.rsna.ctp.objects.DicomObject(request.file);
      FileObject outObject = container.executePipeline(fileObject);
      File inFile = outObject.getFile();
      File originalFile = request.file;

      // Index the file
      DicomObject tags = TagLoader.loadTags(inFile);
      logger.info("Saving file for " + tags.getString(Tag.PatientName));

      File relativePath = constructRelativeFileName(tags);

      File outFile = new File(container.poolDirectory, relativePath.getPath());

      if (!inFile.exists()) {
        logger.error("Input file " + inFile + " does not exist");
        throw new Exception("Input file " + inFile + " does not exist");
      }

      logger.debug("Final path: " + outFile);
      outFile.getParentFile().mkdirs();

      // Insert
      Session session = sessionFactory.getCurrentSession();
      session.beginTransaction();

      try {
        Pool pool = (Pool) session.merge(container.pool);

        Query query;
        query = session.createQuery("from Study where PoolKey = :poolkey and StudyInstanceUID = :suid");
        query.setInteger("poolkey", pool.poolKey);
        query.setString("suid", tags.getString(Tag.StudyInstanceUID));
        Study study = (Study) query.uniqueResult();
        if (study == null) {
          study = new Study(tags);
          study.pool = pool;
          session.saveOrUpdate(study);
        }

        // Find the Instance
        query = session.createQuery("from Series where StudyKey = :studykey and SeriesInstanceUID = :suid").setInteger("studykey", study.StudyKey);
        query.setString("suid", tags.getString(Tag.SeriesInstanceUID));
        Series series = (Series) query.uniqueResult();
        if (series == null) {
          series = new Series(tags);
          series.study = study;
          session.saveOrUpdate(series);
        }

        // Find the Instance
        query = session.createQuery("from Instance where SeriesKey = :serieskey and SOPInstanceUID = :suid").setInteger("serieskey", series.SeriesKey);
        query.setString("suid", tags.getString(Tag.SOPInstanceUID));
        Instance instance = (Instance) query.uniqueResult();
        if (instance == null) {
          instance = new Instance(tags, relativePath.getPath());
          instance.series = series;
          session.saveOrUpdate(instance);
        }

        // Copy the file, remove later
        Files.copy(inFile, outFile);
        logger.debug("Moved file " + inFile + " to " + outFile);

        // Delete the input file, it is not needed any more
        if (inFile.exists()) {
          inFile.delete();
        }
        if (originalFile.exists()) {
          originalFile.delete();
        }

      } catch (Exception e) {
        logger.error("Caught exception", e);
      } finally {
        session.getTransaction().commit();
      }
    }
  }

  static File constructRelativeFileName(DicomObject dataset) {
    String[] pathFormat = SortedFilenameFormat.split("/");
    File relativePath = new File("sorted");
    for (String tag : pathFormat) {
      String t = dataset.getString(Tag.forName(tag));
      if (t == null) {
        t = "UNKNOWN";
      }
      relativePath = new File(relativePath, t);
    }
    return relativePath;
  }

}
