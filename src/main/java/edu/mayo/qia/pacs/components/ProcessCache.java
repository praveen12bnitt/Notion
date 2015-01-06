package edu.mayo.qia.pacs.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessCache {

  public Map<String, Study> studies = new ConcurrentHashMap<String, Study>();
  public Map<String, Series> series = new ConcurrentHashMap<String, Series>();

}
