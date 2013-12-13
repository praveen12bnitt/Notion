package edu.mayo.qia.pacs.components;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table
public final class Pool {

  @Id
  @GeneratedValue
  public int poolKey;

  public String name;
  public String description;
  public String applicationEntityTitle;
  @Column(columnDefinition = "INTEGER")
  public boolean anonymize;

  @JsonIgnore
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "pool")
  // @JoinColumn(name = "PoolKey")
  public Set<Device> devices = new HashSet<Device>();

  @JsonIgnore
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "script")
  // @JoinColumn(name = "PoolKey")
  public Set<Script> scripts = new HashSet<Script>();

  public Pool(String name, String path, String applicationEntityTitle, boolean anonymize) {
    this.name = name;
    this.description = path;
    this.applicationEntityTitle = applicationEntityTitle;
    this.anonymize = anonymize;
  }

  public Pool() {
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
  }
}
