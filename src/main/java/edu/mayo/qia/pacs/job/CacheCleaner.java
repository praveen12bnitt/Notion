package edu.mayo.qia.pacs.job;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import edu.mayo.qia.pacs.Notion;

public class CacheCleaner implements Job {
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    Notion.context.getBean("authorizationCache", Map.class).clear();
  }
}
