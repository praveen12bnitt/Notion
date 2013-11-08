package edu.mayo.qia.pacs.dicom;

import java.io.IOException;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.Status;
import org.dcm4che2.net.service.CMoveSCP;
import org.dcm4che2.net.service.DicomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveSCP extends DicomService implements CMoveSCP {
  static Logger logger = LoggerFactory.getLogger(MoveSCP.class);

  static public String[] PresentationContexts = new String[] { UID.StudyRootQueryRetrieveInformationModelMOVE, UID.PatientRootQueryRetrieveInformationModelMOVE };

  public MoveSCP() {
    super(PresentationContexts);
  }

  @Override
  public void cmove(final Association as, final int pcid, final DicomObject command, DicomObject request) throws DicomServiceException, IOException {

    as.writeDimseRSP(pcid, CommandUtils.mkRSP(command, Status.Success));

  }
}
