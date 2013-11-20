package edu.mayo.qia.pacs.components;

import java.io.File;

import org.apache.log4j.Logger;

import edu.mayo.qia.pacs.PACS;

/**
 * Manage a particular pool.
 * 
 * @author Daniel Blezek
 * 
 */
public class PoolContainer {
  static Logger logger = Logger.getLogger(PoolContainer.class);
  Pool pool;
  File poolDirectory;

  public PoolContainer(Pool pool) {
    this.pool = pool;
    if (this.pool.poolKey <= 0) {
      throw new RuntimeException("PoolKey must be set!");
    }
  }

  public void start() {
    logger.info("Starting up pool: " + pool);

    // See if the directory exists
    poolDirectory = new File(PACS.directory, Integer.toString(pool.poolKey));
    if (!poolDirectory.exists()) {
      poolDirectory.mkdirs();
    }

    // Would start CTP here as well

  }

  public void stop() {
    // Noop
    logger.info("Shutting down pool: " + pool);
  }

  File getPoolDirectory() {
    return poolDirectory;
  }
}
