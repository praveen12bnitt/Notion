package edu.mayo.qia.pacs.components;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class AnonymizationMapProcessor implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    for (PoolContainer container : PoolManager.poolContainers.values()) {
      container.processAnonymizationMap();
    }
  }
}
