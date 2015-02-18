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
    Double v = gauge.getValue();
    assertTrue(v.doubleValue() > 0.0);
    // Sleep 2 seconds
    Thread.sleep(2 * 1000);
    v = gauge.getValue();
    assertTrue(v.doubleValue() == 0.0);

  }

}
