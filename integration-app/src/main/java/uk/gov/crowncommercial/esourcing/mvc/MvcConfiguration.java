package uk.gov.crowncommercial.esourcing.mvc;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MvcConfiguration {

  @Bean
  Clock clock() {
    /* create a Clock so can time can easily be overridden/mocked when unit testing */
    return Clock.systemUTC();
  }
}
