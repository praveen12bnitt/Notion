package edu.mayo.qia.pacs.components;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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

  @JsonIgnore
  @OneToMany
  @JoinColumn(name = "PoolKey")
  public Set<Device> devices = new HashSet<Device>();

  public Pool(String name, String path) {
    this.name = name;
    this.description = path;
  }

  public Pool() {
  }

  public String toString() {
    return "Pool(" + poolKey + "): " + description + ": " + description;
  }

  @JsonIgnore
  public Set<Device> getDevices() {
    return devices;
  }

  // Set my values from somewhere else, but not the key!
  public void update(Pool update) {
    this.name = update.name;
    this.description = update.description;
  }
}
