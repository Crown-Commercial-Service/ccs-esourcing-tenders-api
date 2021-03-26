package uk.gov.crowncommercial.esourcing.integration.service;

import java.util.Map;

public interface EmailService {

  void sendSalesforceUpdateFailureEmail(Map<String, String> map);
}
