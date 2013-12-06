package edu.mayo.qia.pacs.dicom;

/**
 * @author gpotter (gcac96@gmail.com)
 * @version $Revision$
 */
@SuppressWarnings("javadoc")
public class DcmMoveException extends Exception {
  static final long serialVersionUID = 1L;

  public DcmMoveException(String message, Throwable cause) {
    super(message, cause);
  }

  public DcmMoveException(Throwable cause) {
    super(cause);
  }

  public DcmMoveException(String message) {
    super(message, null);
  }
}
