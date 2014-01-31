package edu.mayo.qia.pacs.dicom;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.Status;
import org.dcm4che2.net.service.CMoveSCP;
import org.dcm4che2.net.service.DicomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.components.Device;
import edu.mayo.qia.pacs.dicom.DICOMReceiver.AssociationInfo;

@Component
public class MoveSCP extends DicomService implements CMoveSCP {
  static Logger logger = LoggerFactory.getLogger(MoveSCP.class);

  static public String[] PresentationContexts = new String[] { UID.StudyRootQueryRetrieveInformationModelMOVE, UID.PatientRootQueryRetrieveInformationModelMOVE };

  @Autowired
  JdbcTemplate template;

  @Autowired
  DICOMReceiver dicomReceiver;

  public MoveSCP() {
    super(PresentationContexts);
  }

  @Override
  public void cmove(final Association as, final int pcid, final DicomObject command, DicomObject request) throws DicomServiceException, IOException {

    final AssociationInfo info = dicomReceiver.getAssociationMap().get(as);
    if (info == null) {
      throw new DicomServiceException(request, Status.ProcessingFailure, "Invalid or unknown association");
    }
    if (!info.canConnect) {
      throw new DicomServiceException(request, Status.ProcessingFailure, "AET (" + as.getCalledAET() + ") is unknown");
    }

    // The calling machine can connect, see if we can find the outbound
    // machine...
    final String destinationAET = command.getString(Tag.MoveDestination);
    final Device destination = new Device();
    template.query("select HostName, Port from Device where ApplicationEntityTitle = ? and PoolKey = ?", new Object[] { destinationAET, info.poolKey }, new RowCallbackHandler() {

      @Override
      public void processRow(ResultSet rs) throws SQLException {
        destination.applicationEntityTitle = destinationAET;
        destination.port = rs.getInt("Port");
        destination.hostName = rs.getString("HostName");
      }
    });
    if (destination.applicationEntityTitle == null) {
      // 0xa801 is refused: Move Destination unknown
      as.writeDimseRSP(pcid, CommandUtils.mkRSP(command, 0xa801));
      return;
    }

    // Find the studies to send
    // Find the series we need to move
    String retrieveLevel = request.getString(Tag.QueryRetrieveLevel);
    List<Integer> seriesKeyList = new ArrayList<Integer>();
    if (retrieveLevel.equalsIgnoreCase("STUDY")) {
      String uid = request.getString(Tag.StudyInstanceUID);
      seriesKeyList.addAll(template.queryForList("select SERIES.SeriesKey from SERIES, STUDY where SERIES.StudyKey = STUDY.StudyKey and STUDY.StudyInstanceUID = ? and STUDY.PoolKey = ?", Integer.class, uid, info.poolKey));
    }
    if (retrieveLevel.equalsIgnoreCase("SERIES")) {
      String uid = request.getString(Tag.SeriesInstanceUID);
      seriesKeyList.addAll(template.queryForList("select SERIES.SeriesKey from SERIES, STUDY where SERIES.SeriesInstanceUID = ? and STUDY.StudyKey = SERIES.StudyKey and STUDY.PoolKey = ?", Integer.class, uid, info.poolKey));
    }
    if (seriesKeyList.size() == 0) {
      // 0xa801 is Unable to calculate number of matches
      as.writeDimseRSP(pcid, CommandUtils.mkRSP(command, 0xa701));
      return;
    }

    final DcmSnd sender = new DcmSnd(as.getCalledAET());
    sender.setCalledAET(destination.applicationEntityTitle);
    sender.setRemoteHost(destination.hostName);
    sender.setRemotePort(destination.port);
    sender.setCalling(as.getCalledAET());

    for (Integer key : seriesKeyList) {
      template.query("select FilePath from INSTANCE where SeriesKey = ?", new Object[] { key }, new RowCallbackHandler() {

        @Override
        public void processRow(ResultSet rs) throws SQLException {
          File f = new File(info.poolRootDirectory, rs.getString("FilePath"));
          sender.addFile(f);

        }
      });
    }

    FileMovedHandler callback = new FileMovedHandler() {

      @Override
      public void fileMoved(int current, int total) {
        DicomObject response = CommandUtils.mkRSP(command, Status.Pending);
        response.putInt(Tag.NumberOfCompletedSuboperations, VR.US, current + 1);
        response.putInt(Tag.NumberOfRemainingSuboperations, VR.US, total - current - 1);
        response.putInt(Tag.NumberOfFailedSuboperations, VR.US, 0);
        response.putInt(Tag.NumberOfWarningSuboperations, VR.US, 0);
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Sending " + current + " of " + total + " images");
            logger.debug("Returning Response: \n" + response);
          }
          as.writeDimseRSP(pcid, response);
        } catch (Exception e) {
          logger.error("Failed to write return response", e);
        }

      }
    };
    sender.configureTransferCapability();

    try {
      sender.open();
      sender.send(callback);
    } catch (Exception e) {
      logger.error("ERROR: Failed to send", e);
    } finally {
      sender.close();
    }
    as.writeDimseRSP(pcid, CommandUtils.mkRSP(command, Status.Success));

  }
}
