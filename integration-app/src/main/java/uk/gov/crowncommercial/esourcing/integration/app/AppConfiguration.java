package uk.gov.crowncommercial.esourcing.integration.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.DateFormat;
import java.time.Clock;
import java.util.TimeZone;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RFC3339DateFormat;

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

  @Bean
  ObjectMapper clientObjectMapper() {
    DateFormat dateFormat = new RFC3339DateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(dateFormat);
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JsonNullableModule jnm = new JsonNullableModule();
    mapper.registerModule(jnm);

    return mapper;
  }
}
