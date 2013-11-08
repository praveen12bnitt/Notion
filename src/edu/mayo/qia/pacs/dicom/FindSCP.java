/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Gunter Zeilinger, Huetteldorferstr. 24/10, 1150 Vienna/Austria/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunterze@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package edu.mayo.qia.pacs.dicom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4che2.data.DateRange;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.Status;
import org.dcm4che2.net.service.CFindSCP;
import org.dcm4che2.net.service.DicomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindSCP extends DicomService implements CFindSCP {
  static Logger logger = LoggerFactory.getLogger(FindSCP.class);
  public static String[] PresentationContexts = new String[] { UID.PatientRootQueryRetrieveInformationModelFIND, UID.StudyRootQueryRetrieveInformationModelFIND };

  public FindSCP() {
    super(PresentationContexts);
  }

  void addWhere(int tag, String column, DicomObject data, List<Object> args, StringBuilder query) {
    if (data.contains(tag) && data.containsValue(tag)) {
      query.append(" AND " + column + " like ? ");
      args.add(data.getString(tag).replace("*", "%").replace("?", "_"));
    }
  }

  @Override
  public void cfind(final Association as, final int pcid, DicomObject rq, final DicomObject data) throws DicomServiceException, IOException {

    logger.info("Got request: \n" + rq);
    logger.info("Got data: \n" + data);

    // Find the retrieve level
    if (!data.contains(Tag.QueryRetrieveLevel)) {
      // Write an error
      as.writeDimseRSP(pcid, CommandUtils.mkRSP(rq, Status.ProcessingFailure));
      return;
    }
    final String retrieveLevel = data.get(Tag.QueryRetrieveLevel).getString(null, true);
    logger.info("Retrieve level: " + retrieveLevel);

    // We should send back the RetrieveAET
    final String retrieveAETitle = (as.getLocalAET() == null) ? as.getCalledAET() : as.getLocalAET();

    final DicomObject pending = CommandUtils.mkRSP(rq, Status.Pending);

    if (retrieveLevel.equalsIgnoreCase("SERIES")) {
      if (data.containsValue(Tag.StudyInstanceUID)) {
        final Map<Integer, String> tagColumn = new HashMap<Integer, String>();
        tagColumn.put(Tag.Modality, "Modality");
        tagColumn.put(Tag.InstitutionName, "InstitutionName");
        tagColumn.put(Tag.SeriesDescription, "SeriesDescription");
        tagColumn.put(Tag.SeriesInstanceUID, "SeriesInstanceUID");
        tagColumn.put(Tag.SeriesNumber, "SeriesNumber");
        tagColumn.put(Tag.NumberOfSeriesRelatedInstances, "NumberOfSeriesRelatedInstances");

        final String uid = data.getString(Tag.StudyInstanceUID);
        StringBuilder query = new StringBuilder();
        query.append("select ");
        for (String column : tagColumn.values()) {
          query.append("SERIES." + column + " AS " + column + ", ");
        }

        // Don't want these in the query string
        tagColumn.put(Tag.SeriesDate, "SeriesDate");
        tagColumn.put(Tag.SeriesTime, "SeriesTime");

        query.append(" STUDY.StudyDate as SeriesDate, STUDY.StudyTime as SeriesTime from SERIES, STUDY where STUDY.StudyInstanceUID = ? and STUDY.StudyKey = SERIES.StudyKey");
        logger.info("SERIES Query: " + query);
        logger.info("StudyUID: " + uid);

      }
    }

    if (retrieveLevel.equalsIgnoreCase("PATIENT") || retrieveLevel.equalsIgnoreCase("STUDY")) {
      final Map<Integer, String> tagColumn = new HashMap<Integer, String>();
      // Patient Queries
      tagColumn.put(Tag.PatientName, "Patientname");
      tagColumn.put(Tag.PatientID, "PatientID");
      tagColumn.put(Tag.PatientBirthDate, "PatientBirthDate");
      tagColumn.put(Tag.PatientSex, "PatientSex");
      // Study Queries
      tagColumn.put(Tag.StudyDate, "StudyDate");
      tagColumn.put(Tag.StudyTime, "StudyTime");
      tagColumn.put(Tag.StudyID, "StudyID");
      tagColumn.put(Tag.AccessionNumber, "AccessionNumber");
      tagColumn.put(Tag.StudyInstanceUID, "StudyInstanceUID");
      tagColumn.put(Tag.StudyDescription, "StudyDescription"); // Do a patient
                                                               // query
      tagColumn.put(Tag.NumberOfStudyRelatedInstances, "NumberOfStudyRelatedInstances");
      tagColumn.put(Tag.NumberOfStudyRelatedSeries, "NumberOfStudyRelatedSeries");

      ArrayList<Object> args = new ArrayList<Object>();
      StringBuilder query = new StringBuilder();
      query.append("select");
      StringBuilder tables = new StringBuilder();
      tables.append(" from STUDY"); // Space at beginning to prevent running
                                    // into existing text

      // query.append("select PatientID, Patientname, PatientBirthDate, PatientSex, StudyID, StudyDate, StudyTime, AccessionNumber, StudyInstanceUID, StudyDescription, StudyKey from STUDY "
      // WHERE 1=1 ");
      query.append(" PatientID, Patientname, PatientBirthDate, PatientSex");
      query.append(", StudyID, StudyDate, StudyTime, AccessionNumber, StudyInstanceUID, StudyDescription, STUDY.StudyKey as StudyKey");
      if (data.contains(Tag.NumberOfStudyRelatedSeries) || data.contains(Tag.NumberOfStudyRelatedInstances)) {
        query.append(", count(distinct(SERIES.SeriesKey)) as NumberOfStudyRelatedSeries");
        tables.append(" join SERIES on SERIES.StudyKey = STUDY.StudyKey");
      }
      if (data.contains(Tag.NumberOfStudyRelatedInstances)) {
        query.append(", count(distinct(SOPInstanceUID)) as NumberOfStudyRelatedInstances");
        tables.append(" join INSTANCE on INSTANCE.SeriesKey = SERIES.SeriesKey");
      }
      // Add on the list of tables to query from
      query.append(tables);
      query.append(" where 1=1 ");
      addWhere(Tag.PatientName, tagColumn.get(Tag.PatientName), data, args, query);
      addWhere(Tag.PatientID, tagColumn.get(Tag.PatientID), data, args, query);
      addWhere(Tag.AccessionNumber, tagColumn.get(Tag.AccessionNumber), data, args, query);
      addWhere(Tag.StudyID, tagColumn.get(Tag.StudyID), data, args, query);
      if (data.contains(Tag.StudyDate) && data.containsValue(Tag.StudyDate)) {
        DateRange range = data.getDateRange(Tag.StudyDate);
        if (range.getStart() != null) {
          query.append("AND StudyDate >= ? ");
          args.add(range.getStart());
        }
        if (range.getEnd() != null) {
          query.append(" and StudyDate <= ? ");
          args.add(range.getEnd());
        }
      }
      // Finally, add a group by clause to enable the image/series counting
      query.append(" group by STUDY.StudyKey");

      // Need to handle Dates and times
      try {
        logger.info("Ready to run qeury " + query);
        logger.info("Arguments: " + args);

      } catch (Exception e) {
        logger.error("Error finding patients", e);
        as.writeDimseRSP(pcid, CommandUtils.mkRSP(rq, Status.ProcessingFailure));
        return;
      }
    }
    // All done
    as.writeDimseRSP(pcid, CommandUtils.mkRSP(rq, CommandUtils.SUCCESS), null);
  }

}
