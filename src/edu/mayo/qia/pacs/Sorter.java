package edu.mayo.qia.pacs;

import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.springframework.stereotype.Component;

import edu.mayo.qia.pacs.dicom.TagLoader;
import edu.mayo.qia.pacs.message.ProcessIncomingInstance;

@Component
public class Sorter {

  Logger logger = Logger.getLogger(Sorter.class);

  public static final String SortedFilenameFormat = "AccessionNumber/StudyInstanceUID/SeriesInstanceUID/SOPInstanceUID";

  public void handleMessage(ProcessIncomingInstance request) throws Exception {

    // Index the file
    DicomObject tags = TagLoader.loadTags(request.file);
    logger.info("Saving file for " + tags.getString(Tag.PatientName));

  }
}
