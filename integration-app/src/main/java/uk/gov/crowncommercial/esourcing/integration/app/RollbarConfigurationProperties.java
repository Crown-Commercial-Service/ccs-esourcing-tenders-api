package uk.gov.crowncommercial.esourcing.integration.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rollbar")
public final class RollbarConfigurationProperties {

  private boolean enabled;

  private String accessToken;

  private String environment;

  private String framework;

  private String endpoint;
  
  public boolean isEnabled() {
    return enabled;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getEnvironment() {
    return environment;
  }

  public String getFramework() {
    return framework;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public void setFramework(String framework) {
    this.framework = framework;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

}
