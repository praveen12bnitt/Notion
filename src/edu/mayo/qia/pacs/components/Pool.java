package edu.mayo.qia.pacs.components;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class Pool {

  @Id
  @GeneratedValue
  public int poolKey;

  public String name;
  public String path;

  public Pool(String name, String path) {
    this.name = name;
    this.path = path;
  }

  public Pool() {
  }

  public String toString() {
    return "Pool(" + poolKey + "): " + name + ": " + path;
  }
}
