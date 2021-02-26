package uk.gov.crowncommercial.esourcing.integration.app;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

  @Bean
  Clock clock() {
    /* create a Clock so time can easily be overridden/mocked when unit testing */
    return Clock.systemUTC();
  }

  @Bean
  InfoAppConfigurationProperties infoAppConfigurationProperties() {
    return new InfoAppConfigurationProperties();
  }

  @Bean
  ApplicationStartupCompleteListener applicationStartupCompleteListener() {
    return new ApplicationStartupCompleteListener();
  }

  @Bean
  ActiveProfilesVerifier activeProfilesVerifier() {
    return new ActiveProfilesVerifier(true);
  }
}
