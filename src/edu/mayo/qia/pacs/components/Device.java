package edu.mayo.qia.pacs.components;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table
public final class Device {
  @Id
  @GeneratedValue
  public int deviceKey;
  public String applicationEntityTitle;
  public String hostName;
  public int port;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "PoolKey")
  private Pool pool;

  public Device() {
  }

  public Device(String applicationEntityTitle, String hostName, int port) {
    this(applicationEntityTitle, hostName, port, null);
  }

  public Device(String applicationEntityTitle, String hostName, int port, Pool pool) {
    this.applicationEntityTitle = applicationEntityTitle;
    this.hostName = hostName;
    this.port = port;
    this.pool = pool;
  }

  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

}
