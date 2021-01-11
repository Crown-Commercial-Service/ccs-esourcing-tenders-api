package uk.gov.crowncommercial.esourcing.api;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import uk.gov.crowncommercial.esourcing.service.TenderApiService;

@Configuration
public class MockApiServicesConfiguration {

  @MockBean
  private TenderApiService tenderApiService;
}

