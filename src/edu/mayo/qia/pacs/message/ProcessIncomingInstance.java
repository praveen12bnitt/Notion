package edu.mayo.qia.pacs.message;

import java.io.File;
import java.io.Serializable;

import org.dcm4che2.net.Association;

public class ProcessIncomingInstance implements Serializable {

  private static final long serialVersionUID = 1L;
  public final Association association;
  public final File file;

  public ProcessIncomingInstance(Association association, File file) {
    this.association = association;
    this.file = file;
  }

}
