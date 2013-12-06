package edu.mayo.qia.pacs.ctp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.Processor;
import org.w3c.dom.Element;

public class Anonymizer extends AbstractPipelineStage implements Processor {
  static Logger logger = Logger.getLogger(Anonymizer.class);

  public Anonymizer(Element element) {
    super(element);
  }

  @Override
  public FileObject process(FileObject fileObject) {

    try {
      // Load the tags, replace PatientName, PatientID and AccessionNumber
      DicomInputStream dis = new DicomInputStream(fileObject.getFile());
      DicomObject dcm = dis.readDicomObject();
      dis.close();

      dcm.putString(Tag.PatientName, VR.PN, "empty");
      dcm.putString(Tag.PatientID, VR.LO, "1234");
      dcm.putString(Tag.AccessionNumber, VR.SH, "12345");

      File output = new File(fileObject.getFile().getParentFile(), UUID.randomUUID().toString());
      FileOutputStream fos = new FileOutputStream(output);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      DicomOutputStream dos = new DicomOutputStream(bos);
      dos.writeDicomFile(dcm);
      dos.close();

      fileObject.getFile().delete();
      FileUtils.moveFile(output, fileObject.getFile());
    } catch (Exception e) {
      logger.error("Error changing patient info", e);
    }
    return fileObject;
  }

}
