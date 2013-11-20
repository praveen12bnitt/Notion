package edu.mayo.qia.pacs.dicom;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.dcm4che2.data.UID;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.VerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.PACS;

/**
 * Provides a stand-alone DICOM receiver rather than the heavyweight
 * {@link DcmRcv}.
 * 
 * @author Daniel Blezek
 * 
 */
@Component
@DependsOn("flyway")
public class DICOMReceiver {
  static Logger logger = LoggerFactory.getLogger(DICOMReceiver.class);

  private final Device device = new Device(null);
  private final NetworkApplicationEntity ae = new NetworkApplicationEntity();
  private final NetworkConnection nc = new NetworkConnection();

  @Autowired
  StorageSCP storageSCP;

  private final FindSCP findSCP = new FindSCP();
  private final MoveSCP moveSCP = new MoveSCP();
  private final Executor executor = Executors.newCachedThreadPool();

  private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };

  @Autowired
  JmsTemplate jmsTemplate;

  /** Standard constructor */
  public DICOMReceiver() {
  }

  public synchronized void stop() {
    device.stopListening();
  }

  /**
   * Starts the receiver listening.
   * 
   * @throws Exception
   *           if could not start database
   */
  @PostConstruct
  public synchronized void start() throws Exception {
    logger.info("Starting");
    nc.setPort(PACS.DICOMPort);
    device.setNetworkApplicationEntity(ae);
    device.setNetworkConnection(nc);
    ae.setNetworkConnection(nc);
    ae.setAssociationAcceptor(true);
    ae.setPackPDV(true);
    nc.setTcpNoDelay(true);

    String[] NON_RETIRED_LE_TS = { UID.JPEGLSLossless, UID.JPEGLossless, UID.JPEGLosslessNonHierarchical14, UID.JPEG2000LosslessOnly, UID.DeflatedExplicitVRLittleEndian, UID.RLELossless, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian,
        UID.JPEGBaseline1, UID.JPEGExtended24, UID.JPEGLSLossyNearLossless, UID.JPEG2000, UID.MPEG2, };

    ArrayList<TransferCapability> tc = new ArrayList<TransferCapability>();
    tc.add(new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS, TransferCapability.SCP));
    for (int i = 0; i < StorageSCP.CUIDS.length; i++) {
      tc.add(new TransferCapability(StorageSCP.CUIDS[i], NON_RETIRED_LE_TS, TransferCapability.SCP));
    }

    for (int i = 0; i < FindSCP.PresentationContexts.length; i++) {
      tc.add(new TransferCapability(FindSCP.PresentationContexts[i], NON_RETIRED_LE_TS, TransferCapability.SCP));
    }
    for (int i = 0; i < MoveSCP.PresentationContexts.length; i++) {
      tc.add(new TransferCapability(MoveSCP.PresentationContexts[i], NON_RETIRED_LE_TS, TransferCapability.SCP));
    }

    if (logger.isDebugEnabled()) {
      for (TransferCapability tt : tc) {
        logger.debug("Added: " + tt.getSopClass());
      }
    }
    ae.setTransferCapability(tc.toArray(new TransferCapability[] {}));
    ae.register(new VerificationService());
    ae.register(storageSCP);
    ae.register(findSCP);
    ae.register(moveSCP);

    ae.addAssociationListener(storageSCP);

    device.startListening(executor);

    // if (dcmrcv == null) {
    // dcmrcv = new DcmRcv(aeTitle);
    // dcmrcv.setPort(port);
    // dcmrcv.setDestination(destination);
    // associationInfoCollector.setServer(getServer());
    // associationInfoCollector.setBaseDirectory(destination);
    // String hostName = DeweyUtilities.getHostname();
    // if (hostName == null) {
    // hostName = "unknown";
    // }
    // try {
    // associationInfoCollector.startDB(aeTitle, hostName, port);
    // } catch (Exception e) {
    // logger.error("Error starting DB", e);
    // throw e;
    // }
    // dcmrcv.setCollector(associationInfoCollector);
    // dcmrcv.getNetworkApplicationEntity().addAssociationListener(associationInfoCollector);
    // }
    // dcmrcv.setPackPDV(true);
    // dcmrcv.setTcpNoDelay(true);
    // dcmrcv.initTransferCapability();
    //
    // // DJB Test
    // final String[] NON_RETIRED_LE_TS = { UID.JPEGLSLossless,
    // UID.JPEGLossless, UID.JPEGLosslessNonHierarchical14,
    // UID.JPEG2000LosslessOnly, UID.DeflatedExplicitVRLittleEndian,
    // UID.RLELossless, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian,
    // UID.JPEGBaseline1, UID.JPEGExtended24, UID.JPEGLSLossyNearLossless,
    // UID.JPEG2000, UID.MPEG2, };
    //
    // String[] tsuids = NON_RETIRED_LE_TS;
    //
    // NetworkApplicationEntity ae = dcmrcv.getNetworkApplicationEntity();
    // ae.register(new CFindService());
    // ae.register(new CMoveService());
    // // Append the Transfer Capabilities for QUERY
    // ArrayList<TransferCapability> tc = new
    // ArrayList<TransferCapability>(Arrays.asList(ae.getTransferCapability()));
    // tc.add(new
    // TransferCapability(UID.PatientRootQueryRetrieveInformationModelFIND,
    // tsuids, TransferCapability.SCP));
    // tc.add(new
    // TransferCapability(UID.StudyRootQueryRetrieveInformationModelFIND,
    // tsuids, TransferCapability.SCP));
    // tc.add(new
    // TransferCapability(UID.PatientRootQueryRetrieveInformationModelMOVE,
    // tsuids, TransferCapability.SCP));
    // tc.add(new
    // TransferCapability(UID.StudyRootQueryRetrieveInformationModelMOVE,
    // tsuids, TransferCapability.SCP));
    // ae.setTransferCapability(tc.toArray(ae.getTransferCapability()));
    // dcmrcv.start();
  }

}
