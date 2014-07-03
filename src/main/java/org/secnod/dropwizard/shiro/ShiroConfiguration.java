package org.secnod.dropwizard.shiro;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShiroConfiguration {

  @NotNull
  @JsonProperty("iniConfigs")
  private String[] iniConfigs = new String[] { "classpath:shiro.ini" };

  @JsonProperty("filterUrlPattern")
  private String filterUrlPattern = "/*";

  public String[] getIniConfigs() {
    return iniConfigs;
  }

  public String getFilterUrlPattern() {
    return filterUrlPattern;
  }
}
