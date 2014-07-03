package edu.mayo.qia.pacs.configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerConfiguration {
  @Valid
  @JsonProperty
  public String host;

  @Valid
  @JsonProperty
  public String templatePath;

  @Valid
  @JsonProperty
  @NotNull
  public int dicomPort;

  @Valid
  @JsonProperty
  public String hashAlgorithm = "SHA-512";

  @Valid
  @JsonProperty
  public int hashIterations = 10000;

  @Valid
  @JsonProperty
  @NotNull
  public String imageDirectory;

  @Valid
  @JsonProperty
  public Boolean allowRegistration = Boolean.TRUE;

  public String getHost() {
    return host;
  }

  public String getTemplatePath() {
    return templatePath;
  }
}
