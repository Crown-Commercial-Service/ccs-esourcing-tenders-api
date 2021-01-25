package uk.gov.crowncommercial.esourcing.integration.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.rollbar.notifier.sender.Sender;

@Configuration
public class IntegrationTestConfig {

  @MockBean
  private Sender mockSender;

  @Bean
  @Primary
  Supplier<Sender> mockSenderSupplier() {
    /* override the Sender so we can intercept rollbar sends and mock as needed */ 
    return () -> mockSender;
  }

  @Bean
  @Primary
  Clock fixedClock() {
    /* for most tests a repeatable / fixed clock is useful, if need anything more may need to mock it */ 
    return Clock.fixed(Instant.ofEpochSecond(1610454603L), ZoneId.of("UTC"));
  }
}
