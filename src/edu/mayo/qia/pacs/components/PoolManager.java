package edu.mayo.qia.pacs.components;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

import edu.mayo.qia.pacs.db.ImportHelper;
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
  ConcurrentMap<String, Integer> instanceUIDs = new ConcurrentHashMap<String, Integer>();
  ConcurrentMap<String, Integer> seriesUIDs = new ConcurrentHashMap<String, Integer>();
  ConcurrentMap<String, Integer> studyUIDs = new ConcurrentHashMap<String, Integer>();
  ScheduledFuture<?> future;
  Map<String, PoolContainer> poolContainers = new HashMap<String, PoolContainer>();

  @Autowired
  JdbcTemplate template;

  @Autowired
  TaskScheduler taskScheduler;

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  ImportHelper importHelper;

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

  /** Handle a request to create an image */
  public void handleMessage(ProcessIncomingInstance request) throws Exception {

    // Index the file
    DicomObject tags = TagLoader.loadTags(request.file);
    logger.info("Saving file for " + tags.getString(Tag.PatientName));

    // Pull out the filename
    PoolContainer container = poolContainers.get(request.association.getCalledAET());
    if (container == null) {
      // Now what?
    }
    File inFile = request.file;
    File relativePath = importHelper.constructRelativeFileName(tags);

    File outFile = new File(container.poolDirectory, relativePath.getPath());

    if (!inFile.exists()) {
      logger.error("Input file " + inFile + " does not exist");
      throw new Exception("Input file " + inFile + " does not exist");
    }

    outFile = new File(outFile, relativePath.getPath());
    logger.debug("Final path: " + outFile);
    outFile.getParentFile().mkdirs();

    // Copy the file, remove later
    Files.copy(inFile, outFile);
    logger.debug("Moved file " + inFile + " to " + outFile);
    // Put it in the database
    // Create the Study if necessary
    int studyKey = importHelper.insertStudy(tags, studyUIDs);
    int seriesKey = importHelper.insertSeries(tags, studyKey, seriesUIDs);
    int instanceKey = importHelper.insertInstance(tags, seriesKey, relativePath, instanceUIDs);

    // Delete the input file, it is not needed any more
    inFile.delete();

  }

}
