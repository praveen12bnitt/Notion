package edu.mayo.qia.pacs.managed;

import io.dropwizard.lifecycle.Managed;

import org.quartz.Scheduler;

public class QuartzManager implements Managed {

  public Scheduler scheduler;

  public QuartzManager(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void start() throws Exception {
    scheduler.start();

  }

  @Override
  public void stop() throws Exception {
    scheduler.shutdown(true);
  }

}
