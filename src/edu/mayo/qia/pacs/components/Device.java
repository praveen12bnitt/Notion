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
  public int poolKey;
  public String applicationEntityTitle;
  public String hostName;
  public int port;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "PoolKey", insertable = false, updatable = false)
  private Pool pool;

  public Pool getPool() {
    return pool;
  }

}
