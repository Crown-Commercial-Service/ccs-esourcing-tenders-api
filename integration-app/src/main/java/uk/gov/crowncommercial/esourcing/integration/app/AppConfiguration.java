package uk.gov.crowncommercial.esourcing.integration.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.DateFormat;
import java.time.Clock;
import java.util.TimeZone;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ClientResponse.Builder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ApiClient;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ProjectsApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RFC3339DateFormat;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxApi;

@Configuration
public class AppConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppConfiguration.class);

  // TODO Work out how to get the bearer token
  @Value("${jaggaer.bearer.token:}")
  private String bearerToken;

  @Value("${ccs.esourcing.jaggaer-client-url}")
  private String JAGGAER_CLIENT_URL;

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

  @Bean(name = "jaggaerClientRegistrationRepo")
  ReactiveClientRegistrationRepository getRegistration(
      @Value("${spring.security.oauth2.client.provider.jaggaer.token-uri}") String tokenUri,
      @Value("${spring.security.oauth2.client.registration.jaggaer.client-id}") String clientId,
      @Value("${spring.security.oauth2.client.registration.jaggaer.client-secret}")
          String clientSecret) {
    ClientRegistration clientRegistrations =
        ClientRegistration.withRegistrationId("jaggaer")
            .tokenUri(tokenUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    return new InMemoryReactiveClientRegistrationRepository(clientRegistrations);
  }

  @Bean
  @Scope("prototype")
  public ApiClient apiClient(
      @Qualifier("jaggaerClientRegistrationRepo")
          ReactiveClientRegistrationRepository clientRegistrations) {
    CustomJaggaerClient apiClient = CustomJaggaerClient.createClient(clientRegistrations);
    apiClient.setBasePath(JAGGAER_CLIENT_URL);
    return apiClient;
  }

  @Bean
  @Scope("prototype")
  public ProjectsApi projectsApi(ApiClient apiClient) {

    ProjectsApi projectsApi = new ProjectsApi();

    projectsApi.setApiClient(apiClient);

    return projectsApi;
  }

  @Bean
  @Scope("prototype")
  public RfxApi rfxApi(ApiClient apiClient) {

    RfxApi rfxApi = new RfxApi();

    rfxApi.setApiClient(apiClient);

    return rfxApi;
  }

  private static class CustomJaggaerClient extends ApiClient {

    static CustomJaggaerClient createClient(
        ReactiveClientRegistrationRepository clientRegistrations) {

      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
          new ServerOAuth2AuthorizedClientExchangeFilterFunction(
              clientRegistrations, new UnAuthenticatedServerOAuth2AuthorizedClientRepository());
      oauth.setDefaultClientRegistrationId("jaggaer");

      ExchangeFilterFunction tokenResponseFilter =
          ExchangeFilterFunction.ofResponseProcessor(
              response -> {
                Builder builder = ClientResponse.from(response);

                LOGGER.info("*********** Response: {}", response.statusCode());
                response
                    .headers()
                    .asHttpHeaders()
                    .forEach(
                        (name, values) ->
                            values.forEach(value -> LOGGER.info("{}={}", name, value)));

                return Mono.just(response);
              });
      oauth.andThen(tokenResponseFilter);

      DateFormat dateFormat = new RFC3339DateFormat();
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      ObjectMapper mapper = new ObjectMapper();
      mapper.setDateFormat(dateFormat);
      mapper.registerModule(new JavaTimeModule());
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      JsonNullableModule jnm = new JsonNullableModule();
      mapper.registerModule(jnm);

      return new CustomJaggaerClient(mapper, dateFormat, oauth);
    }

    private CustomJaggaerClient(
        ObjectMapper mapper,
        DateFormat dateFormat,
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth) {
      super(buildCustomWebClient(mapper, oauth), mapper, dateFormat);
    }

    private static WebClient buildCustomWebClient(
        ObjectMapper mapper, ServerOAuth2AuthorizedClientExchangeFilterFunction oauth) {

      ExchangeStrategies strategies =
          ExchangeStrategies.builder()
              .codecs(
                  clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer
                        .defaultCodecs()
                        .jackson2JsonEncoder(
                            new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer
                        .defaultCodecs()
                        .jackson2JsonDecoder(
                            new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
                  })
              .build();

      WebClient.Builder webClient = WebClient.builder().exchangeStrategies(strategies);
      return webClient.filter(oauth).build();
    }
  }
}
