package edu.mayo.qia.pacs.components;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public class Script {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int scriptKey = -1;
  public String script;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "PoolKey")
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Pool pool;

  public Script() {
  }

  public Script(String script) {
    this.script = script;
  }

  public Script(Pool pool, String script) {
    this(script);
    this.pool = pool;
  }

  @JsonIgnore
  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

  public void update(Script inScript) {
    script = inScript.script;
  }

  public static String createDefaultScript() {
    return "// Default anonymization script\n" + " var tags = {\n" + "   PatientName: anonymizer.lookup('PatientName', tags.PatientName ) || 'PN-' + anonymizer.sequenceNumber ( 'PatientName', tags.PatientName),\n"
        + "   PatientID: anonymizer.lookup('PatientID', tags.PatientID ) || anonymizer.sequenceNumber ( 'PatientID', tags.PatientID),\n"
        + "   AccessionNumber: anonymizer.lookup('AccessionNumber', tags.AccessionNumber ) || anonymizer.sequenceNumber ( 'AccessionNumber', tags.AccessionNumber),\n" + " };\n" + " // 1234\n" + "tags;\n";
  }
}
