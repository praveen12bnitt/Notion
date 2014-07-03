package edu.mayo.qia.pacs.job;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import edu.mayo.qia.pacs.Notion;

public class CacheCleaner implements Job {
  public static void clean() {
    Notion.context.getBean("authorizationCache", Map.class).clear();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    clean();
  }
}
