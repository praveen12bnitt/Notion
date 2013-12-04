package edu.mayo.qia.pacs.dicom;

public interface FileMovedHandler {

  /** Called when a file 'current' of 'total' is sent to remote destination. */
  void fileMoved(int current, int total);

}
