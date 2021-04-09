package uk.gov.crowncommercial.esourcing.integration.exception;

import java.util.List;

public class SalesforceUpdateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final List<SalesforceError> salesforceErrorList;

  public SalesforceUpdateException(List<SalesforceError> salesforceErrorList) {
    this.salesforceErrorList = salesforceErrorList;
  }

  public List<SalesforceError> getSalesforceErrorList() {
    return salesforceErrorList;
  }
}
