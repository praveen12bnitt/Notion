package edu.mayo.qia.pacs.components;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public class Script {

  @Id
  @GeneratedValue
  public int scriptKey = -1;
  public String tag;
  public String script;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "PoolKey")
  @JsonIgnore
  private Pool pool;

  public Script() {
  }

  public Script(String tag, String script) {
    this.tag = tag;
    this.script = script;
  }

  public Script(Pool pool, String tag, String script) {
    this(tag, script);
    this.pool = pool;
  }

  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

  public void update(Script inScript) {
    script = inScript.script;
  }

}
