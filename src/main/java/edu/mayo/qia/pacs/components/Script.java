package edu.mayo.qia.pacs.components;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
public class Script {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int scriptKey = -1;
  public String tag;
  public String script;

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "PoolKey")
  @com.fasterxml.jackson.annotation.JsonIgnore
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

  @JsonIgnore
  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

  public void update(Script inScript) {
    script = inScript.script;
  }

  public static String createDefaultScript(String tag, String prefix) {
    StringBuffer s = new StringBuffer();
    s.append("// Lookup a value for this tag\n");
    s.append("var newTag = anonymizer.lookup('" + tag + "', tags." + tag + ");\n");
    s.append("// If we do not have the value, generate a new value\n");
    s.append("if (!newTag) {\n");
    if (prefix != null) {
      s.append("  newTag = '" + prefix + "' + anonymizer.sequenceNumber('" + tag + "', tags." + tag + ");\n");
    } else {
      s.append("  newTag = anonymizer.sequenceNumber('" + tag + "', tags." + tag + ");\n");
    }
    s.append("}\n\n");
    s.append("// Return the new tag\n");
    s.append("newTag\n");
    return s.toString();
  }
}
