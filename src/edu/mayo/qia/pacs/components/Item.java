package edu.mayo.qia.pacs.components;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Entity
@Table(name = "queryitem")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {

  @Id
  @GeneratedValue
  public int queryItemKey = -1;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "QueryKey")
  @JsonIgnore
  public Query query;

  public String status;
  public String patientName;
  public String patientID;
  public String accessionNumber;
  public String patientBirthDate;
  public String studyDate;
  public String modalitiesInStudy;
  public String studyDescription;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "item")
  public Set<Result> items = new HashSet<Result>();

  @JsonIgnore
  public Map<String, String> getTagMap() {
    Map<String, String> map = new HashMap<String, String>();
    // @formatter:off
    if ( patientName != null ) { map.put("PatientName", patientName); }
    if ( patientName != null ) { map.put ("PatientName", patientName ); }
    if ( patientID != null ) { map.put ("PatientID", patientID ); }
    if ( accessionNumber != null ) { map.put ("AccessionNumber", accessionNumber ); }
    if ( patientBirthDate != null ) { map.put ("PatientBirthDate", patientBirthDate ); }
    if ( studyDate != null ) { map.put ("StudyDate", studyDate ); }
    if ( modalitiesInStudy != null ) { map.put ("ModalitiesInStudy", modalitiesInStudy ); }
    if ( studyDescription != null ) { map.put ("StudyDescription", studyDescription ); }
    // @formatter:on
    return map;
  }

}
