package edu.mayo.qia.pacs.dicom;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.AssociationAcceptEvent;
import org.dcm4che2.net.AssociationCloseEvent;
import org.dcm4che2.net.AssociationListener;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.VerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.PACS;
import edu.mayo.qia.pacs.components.PoolManager;

/**
 * Provides a stand-alone DICOM receiver rather than the heavyweight
 * {@link DcmRcv}.
 * 
 * @author Daniel Blezek
 * 
 */
@Component("dicomReceiver")
@DependsOn("flyway")
public class DICOMReceiver implements AssociationListener {
  static Logger logger = LoggerFactory.getLogger(DICOMReceiver.class);
  private ConcurrentHashMap<Association, AssociationInfo> associationMap = new ConcurrentHashMap<Association, AssociationInfo>();

  private final Device device = new Device(null);
  private final NetworkApplicationEntity ae = new NetworkApplicationEntity();
  private final NetworkConnection nc = new NetworkConnection();

  @Autowired
  StorageSCP storageSCP;

  @Autowired
  JdbcTemplate template;

  @Autowired
  PoolManager poolManager;

  @Autowired
  FindSCP findSCP;

  @Autowired
  MoveSCP moveSCP;

  private final Executor executor = Executors.newCachedThreadPool();

  private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };

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

    ae.addAssociationListener(this);

    device.startListening(executor);

  }

  @Override
  public void associationAccepted(AssociationAcceptEvent event) {
    // Check to see if this AE can connect
    final AssociationInfo info = new AssociationInfo();
    final Association association = event.getAssociation();
    associationMap.put(association, info);
    File incoming = new File(PACS.directory, "incoming");
    info.incomingRootDirectory = new File(incoming, event.getAssociation().getCalledAET());
    info.incomingRootDirectory.mkdirs();

    final String remoteHostName = association.getSocket().getInetAddress().getHostName();
    final String callingAET = association.getCallingAET();

    template.query("select Device.ApplicationEntityTitle AS AET,  Device.HostName AS HN, Pool.PoolKey as PK from Device, Pool where Device.PoolKey = Pool.PoolKey and Pool.ApplicationEntityTitle = ?", new Object[] { event.getAssociation().getCalledAET() },
        new RowCallbackHandler() {

          @Override
          public void processRow(ResultSet rs) throws SQLException {
            String AET = rs.getString("AET");
            String HN = rs.getString("HN");
            info.poolKey = rs.getInt("PK");
            if (remoteHostName.matches(HN) && callingAET.matches(AET)) {
              info.canConnect = true;
            }
          }
        });
    if (info.canConnect && poolManager.getContainer(association.getCalledAET()) != null) {
      info.poolRootDirectory = poolManager.getContainer(association.getCalledAET()).getPoolDirectory();
    }
  }

  @Override
  public void associationClosed(AssociationCloseEvent event) {
    associationMap.remove(event.getAssociation());
  }

  static class AssociationInfo {
    public boolean canConnect = false;
    File incomingRootDirectory;
    File poolRootDirectory;
    int poolKey;
  }

  public Map<Association, AssociationInfo> getAssociationMap() {
    return associationMap;
  }

}
