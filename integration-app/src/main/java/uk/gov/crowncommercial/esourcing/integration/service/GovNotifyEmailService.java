package uk.gov.crowncommercial.esourcing.integration.service;

import com.rollbar.notifier.Rollbar;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.integration.exception.SalesforceError;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Service
public class GovNotifyEmailService implements EmailService {

  private static final Logger LOG = LoggerFactory.getLogger(GovNotifyEmailService.class);

  final Rollbar rollbar;

  @Value("${ccs.esourcing.gov.notify.apikey}")
  private String govNotifyApiKey;

  @Value("${ccs.esourcing.gov.notify.techops.email}")
  private String techopsEmail;

  @Value("${ccs.esourcing.gov.notify.salesforce.template.id}")
  private String salesforceTemplateId;

  public GovNotifyEmailService(Rollbar rollbar) {
    this.rollbar = rollbar;
  }

  public void sendSalesforceUpdateFailureEmails(List<SalesforceError> salesforceErrorList) {
    NotificationClient client = new NotificationClient(govNotifyApiKey);
    final LocalDateTime now = LocalDateTime.now();
    salesforceErrorList.forEach(
        e -> prepareMessage(client, now, e.getProcurementRef(), e.getOwnerUserLogin()));
  }

  private void prepareMessage(
      NotificationClient client,
      LocalDateTime now,
      String procurmentReference,
      String emailAddress) {

    LOG.info("Sending email to {}", emailAddress);
    Map<String, Object> personalisation = new HashMap<>();
    personalisation.put("name", emailAddress.substring(0, emailAddress.indexOf("@")));
    personalisation.put("title", "Salesforce status update failure");
    personalisation.put(
        "responsecodemessage", String.format("Procurement Reference - %s", procurmentReference));
    personalisation.put("issuetext", now);

    sendMessage(client, techopsEmail, personalisation);
    sendMessage(client, emailAddress, personalisation);
  }

  private void sendMessage(
      NotificationClient client, String emailAddress, Map<String, Object> personalisation) {
    try {
      client.sendEmail(salesforceTemplateId, emailAddress, personalisation, null);
    } catch (NotificationClientException e) {
      rollbar.error(String.format("Failed to send email to %s", emailAddress));
    }
  }
}
