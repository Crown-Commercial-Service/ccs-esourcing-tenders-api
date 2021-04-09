package uk.gov.crowncommercial.esourcing.integration.service;

import java.util.List;
import uk.gov.crowncommercial.esourcing.integration.exception.SalesforceError;

public interface EmailService {

  void sendSalesforceUpdateFailureEmails(List<SalesforceError> salesforceErrorList);
}
