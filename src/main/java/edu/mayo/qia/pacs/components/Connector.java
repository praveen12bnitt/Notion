package edu.mayo.qia.pacs.components;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Connector {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int connectorKey;

  public String name;
  public String description;

  public int queryPoolKey;
  public int destinationPoolKey;
  public int queryDeviceKey;

  /*
   * @ManyToOne(cascade = { CascadeType.REMOVE, CascadeType.MERGE })
   * 
   * @JoinColumn(name = "QueryPoolKey") public Pool queryPool;
   * 
   * @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
   * 
   * @JoinColumn(name = "DestinationPoolKey") public Pool destinationPool;
   * 
   * @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
   * 
   * @JoinColumn(name = "QueryDeviceKey") public Device queryDeviceKey;
   */
}