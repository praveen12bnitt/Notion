package edu.mayo.qia.pacs.components;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import edu.mayo.qia.pacs.Notion;

public class AnonymizationMapProcessor implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    PoolManager poolManager = Notion.context.getBean("poolManager", PoolManager.class);
    for (PoolContainer container : poolManager.poolContainers.values()) {
      container.processAnonymizationMap();
    }
  }
}
