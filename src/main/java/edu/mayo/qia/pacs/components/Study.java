package edu.mayo.qia.pacs.components;

import java.util.Date;
import java.util.HashSet;
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
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

@Entity
@Table
public class Study {

  @Id
  @GeneratedValue
  public int StudyKey;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "PoolKey")
  public Pool pool;

  @JsonIgnore
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "study")
  // @JoinColumn(name = "PoolKey")
  public Set<Series> series = new HashSet<Series>();

  public String StudyInstanceUID;
  public String AccessionNumber = "";
  public String PatientID = "";
  public String PatientName = "";
  public Date PatientBirthDate;
  public String PatientSex = "";
  public String StudyID = "";
  public Date StudyDate;
  public Date StudyTime;

  public String ReferringPhysicianName;
  public String StudyDescription;

  public Study() {
  }

  public Study(DicomObject tags) {
    StudyInstanceUID = tags.getString(Tag.StudyInstanceUID);
    this.update(tags);
  }

  public void update(DicomObject tags) {
    AccessionNumber = tags.getString(Tag.AccessionNumber);
    PatientID = tags.getString(Tag.PatientID);
    PatientName = tags.getString(Tag.PatientName);
    PatientBirthDate = tags.getDate(Tag.PatientBirthDate);
    PatientSex = tags.getString(Tag.PatientSex);
    StudyID = tags.getString(Tag.StudyID);
    StudyDate = tags.getDate(Tag.StudyDate);
    StudyTime = tags.getDate(Tag.StudyTime);
    ReferringPhysicianName = tags.getString(Tag.ReferringPhysicianName);
    StudyDescription = tags.getString(Tag.StudyDescription);
  }

}
