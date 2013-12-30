package edu.mayo.qia.pacs.components;

import java.io.File;
import java.io.IOException;
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
import org.dcm4che2.net.Association;
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

import edu.mayo.qia.pacs.PACS;
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
        for (PoolContainer container : poolContainers.values()) {
          container.clearMaps();
        }
      }
    }, 5000);
  }

  public void newPool(Pool pool) {
    PoolContainer poolContainer = PACS.context.getBean(PoolContainer.class);
    poolContainers.put(pool.applicationEntityTitle, poolContainer);
    logger.info("Starting pool: " + pool);
    poolContainer.start(pool);

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
  public void processIncomingFile(Association association, File incoming) throws Exception {

    // Pull out the filename
    PoolContainer container = poolContainers.get(association.getCalledAET());
    if (container == null) {
      throw new Exception("Did not find containing for the Called AET");
    }

    container.process(incoming);
  }

  /** Update the PoolContainer, because the pool may have changed */
  public void update(Pool pool) {
    getContainer(pool.poolKey).update(pool);
  }

  public void remove(PoolContainer poolContainer) {
    boolean v = poolContainers.remove(poolContainer.getPool().applicationEntityTitle, poolContainer);
    if (!v) {
      throw new RuntimeException("Could not remove pool for " + poolContainer.getPool().applicationEntityTitle);
    }
  }
}
