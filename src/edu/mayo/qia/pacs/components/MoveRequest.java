package edu.mayo.qia.pacs.components;

import java.util.ArrayList;
import java.util.List;

public class MoveRequest {
  public int destinationPoolKey;
  public List<Integer> studyKeys = new ArrayList<Integer>();
}
