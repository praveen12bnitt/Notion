package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.mayo.qia.pacs.metric.RateGauge;

public class RateGaugeTest extends PACSTest {

  @Test
  public void oneSecond() throws InterruptedException {
    RateGauge gauge = new RateGauge();

    gauge.mark();
    gauge.mark();
    gauge.mark();

    assertTrue(gauge.getValue() > 0.0);
    // Sleep 2 seconds
    Thread.sleep(2 * 1000);
    assertTrue(gauge.getValue() == 0.0);

  }

}
