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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

@Entity
@Table
public final class Pool {

  @JsonProperty("id")
  @Id
  @GeneratedValue
  public int poolKey;

  public String name;
  public String description;
  public String applicationEntityTitle;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "pool")
  // @JoinColumn(name = "PoolKey")
  public Set<Device> devices = new HashSet<Device>();

  public Pool(String name, String path, String applicationEntityTitle) {
    this.name = name;
    this.description = path;
    this.applicationEntityTitle = applicationEntityTitle;
  }

  public Pool() {
  }

  @JsonProperty("links")
  public Map<String, String> getLinks() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("devices", "/pool/" + poolKey + "/device");
    return map;
  }

  public String toString() {
    return this.name + "(" + poolKey + ") " + "description: " + description + " AETitle: " + applicationEntityTitle;
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
