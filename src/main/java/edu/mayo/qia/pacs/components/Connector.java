package edu.mayo.qia.pacs.components;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Connector {
  @Id
  @GeneratedValue
  public int connectorKey = -1;

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