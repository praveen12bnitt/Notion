package edu.mayo.qia.pacs.components;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupRole {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int groupRoleKey;
  public Boolean isPoolAdmin = false;
  public Boolean isCoordinator = false;

  public int poolKey;
  public int groupKey;

}
