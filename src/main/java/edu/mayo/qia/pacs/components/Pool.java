package edu.mayo.qia.pacs.components;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.mayo.qia.pacs.PACS;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Pool {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int poolKey;

  public String name;
  public String description;
  public String applicationEntityTitle;
  @Column(columnDefinition = "INTEGER")
  public boolean anonymize;

  @Transient
  public int port = PACS.DICOMPort;

  @JsonIgnore
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "pool")
  // @JoinColumn(name = "PoolKey")
  public Set<Device> devices = new HashSet<Device>();

  @JsonIgnore
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "pool")
  // @JoinColumn(name = "PoolKey")
  public Set<Script> scripts = new HashSet<Script>();

  public Pool(String name, String path, String applicationEntityTitle, boolean anonymize) {
    this.name = name;
    this.description = path;
    this.applicationEntityTitle = applicationEntityTitle;
    this.anonymize = anonymize;
    this.port = PACS.DICOMPort;
  }

  public Pool() {
    this.port = PACS.DICOMPort;
  }

  public String toString() {
    return this.name + "(" + poolKey + ") " + "description: " + description + " AETitle: " + applicationEntityTitle;
  }

  @JsonIgnore
  public Set<Device> getDevices() {
    return devices;
  }

  @JsonIgnore
  public Set<Script> getScripts() {
    return scripts;
  }

  // Set my values from somewhere else, but not the key!
  public void update(Pool update) {
    this.name = update.name;
    this.description = update.description;
    this.anonymize = update.anonymize;
  }
}
