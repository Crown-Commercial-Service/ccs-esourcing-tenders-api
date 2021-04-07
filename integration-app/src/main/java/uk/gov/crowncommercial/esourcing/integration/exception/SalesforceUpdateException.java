package uk.gov.crowncommercial.esourcing.integration.exception;

import java.util.Map;

public class SalesforceUpdateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Map<String, String> map;


  public SalesforceUpdateException(Map<String, String> map) {
    this.map = map;
  }

  public Map<String, String> getMap() {
    return map;
  }
}
