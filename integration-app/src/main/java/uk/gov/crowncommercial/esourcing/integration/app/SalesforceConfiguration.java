package uk.gov.crowncommercial.esourcing.integration.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.crowncommercial.esourcing.salesforce.client.ApiClient;
import uk.gov.crowncommercial.esourcing.salesforce.client.RfxStatusListApi;

@Configuration
public class SalesforceConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceConfiguration.class);

  @Value("${ccs.esourcing.salesforce.client-url}")
  private String salesforceClientUrl;

  @Bean
  public ApiClient salesforceApiClient() {

    ApiClient apiClient = new ApiClient();

    apiClient.setBasePath(salesforceClientUrl);
    LOGGER.info("Using Saleforce Endpoint - {}", salesforceClientUrl);

    return apiClient;
  }

  @Bean
  public RfxStatusListApi rfxStatusApi(ApiClient apiClient) {
    RfxStatusListApi rfxStatusListApi = new RfxStatusListApi();
    rfxStatusListApi.setApiClient(apiClient);
    return rfxStatusListApi;
  }
}
