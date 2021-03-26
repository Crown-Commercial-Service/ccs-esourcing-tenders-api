package uk.gov.crowncommercial.esourcing.integration.service;

import java.util.Map;

public class SalesforceUpdateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Map map;


  public SalesforceUpdateException(Map<String, String> map) {
    this.map = map;
  }

  public Map getMap() {
    return map;
  }
}
