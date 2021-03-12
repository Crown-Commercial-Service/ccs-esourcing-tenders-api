package uk.gov.crowncommercial.esourcing.integration.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ApiClient;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ProjectsApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RFC3339DateFormat;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxWorkflowsApi;

@Configuration
public class JaggaerConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(JaggaerConfiguration.class);

  @Value("${ccs.esourcing.jaggaer.client-url}")
  private String jaggaerClientUrl;

  /*
   * Create a new Client Registration Repository to hold Oauth credentials.
   * Also add Jaggaer Client Registration using client creds from environment variables
   */
  @Bean
  ReactiveClientRegistrationRepository jaggaerClientRegistrationRepository(
      @Value("${spring.security.oauth2.client.provider.jaggaer.token-uri}") String tokenUri,
      @Value("${spring.security.oauth2.client.registration.jaggaer.client-id}") String clientId,
      @Value("${spring.security.oauth2.client.registration.jaggaer.client-secret}")
          String clientSecret) {
    ClientRegistration jaggaerClientRegistration =
        ClientRegistration.withRegistrationId("jaggaer")
            .tokenUri(tokenUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    return new InMemoryReactiveClientRegistrationRepository(jaggaerClientRegistration);
  }

  /**
   * Extend the auto-generated APIClient to allow for adding Oauth credentials.
   *
   * <p>Also, override the token response parameters.
   *
   * @param jaggaerClientRegistrationRepository ReactiveClientRegistrationRepository
   * @return ApiClient
   */
  @Bean
  public ApiClient jaggaerApiClient(
      ReactiveClientRegistrationRepository jaggaerClientRegistrationRepository) {

    /*
     Create new webclient to be used in combination with an Exchange Function Filter to
     intercept the token response before it is processed.  This is required because the
     parameter names (token & expire_in) in the Jaggaer token response do not conform
     to the Oauth standards.
    */
    ReactiveOAuth2AuthorizedClientService authorizedClientService =
        new InMemoryReactiveOAuth2AuthorizedClientService(jaggaerClientRegistrationRepository);

    AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            jaggaerClientRegistrationRepository, authorizedClientService);

    WebClientReactiveClientCredentialsTokenResponseClient
        webClientReactiveClientCredentialsTokenResponseClient =
            new WebClientReactiveClientCredentialsTokenResponseClient();

    WebClient accessTokenWebClient =
        WebClient.builder().filter(new JaggaerExchangeFilterFunction()).build();

    webClientReactiveClientCredentialsTokenResponseClient.setWebClient(accessTokenWebClient);

    ReactiveOAuth2AuthorizedClientProvider reactiveOAuth2AuthorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials(
                consumer ->
                    consumer.accessTokenResponseClient(
                        webClientReactiveClientCredentialsTokenResponseClient))
            .build();
    authorizedClientManager.setAuthorizedClientProvider(reactiveOAuth2AuthorizedClientProvider);

    /*
     * This code is largely duplicated from the autogenerated Jaggaer API code
     */
    DateFormat dateFormat = new RFC3339DateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(dateFormat);
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JsonNullableModule jnm = new JsonNullableModule();
    mapper.registerModule(jnm);
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

    /*
     * Add the oauth filter to the webclient which will be used during api authentication
     */
    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

    oauth.setDefaultClientRegistrationId("jaggaer");
    WebClient.Builder webClientBuilder = WebClient.builder().exchangeStrategies(strategies);
    WebClient webClient = webClientBuilder.filter(oauth).build();
    ApiClient apiClient = new ApiClient(webClient, mapper, dateFormat);

    apiClient.setBasePath(jaggaerClientUrl);
    LOGGER.info("Using Jaggaer Endpoint - {}", jaggaerClientUrl);
    return apiClient;
  }

  @Bean
  public ProjectsApi jaggaerProjectsApi(ApiClient apiClient) {
    ProjectsApi projectsApi = new ProjectsApi();
    projectsApi.setApiClient(apiClient);
    return projectsApi;
  }

  @Bean
  public RfxApi jaggaerRfxApi(ApiClient apiClient) {
    RfxApi rfxApi = new RfxApi();
    rfxApi.setApiClient(apiClient);
    return rfxApi;
  }

  @Bean
  public RfxWorkflowsApi jaggaerRfxWorkflowsApi(ApiClient apiClient) {
    RfxWorkflowsApi rfxWorkflowsApi = new RfxWorkflowsApi();
    rfxWorkflowsApi.setApiClient(apiClient);
    return rfxWorkflowsApi;
  }

  /**
   * This class generates an Exchange Filter Function to intercept the token response and check and
   * replace non-standard Oauth parameter names
   */
  private static final class JaggaerExchangeFilterFunction implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
      return next.exchange(request)
          .flatMap(
              response -> {
                if (!isJsonCompatibleResponse(response)) {
                  LOGGER.warn(
                      "Content type of access token response is not compatible with {}",
                      MediaType.APPLICATION_JSON);
                  return Mono.just(response);
                }
                return readParameters(response)
                    .map(this::addTokenTypeIfNecessary)
                    .map(params -> createNewResponse(response, params));
              });
    }

    private boolean isJsonCompatibleResponse(ClientResponse response) {
      return response
          .headers()
          .contentType()
          .map(mediaType -> mediaType.isCompatibleWith(MediaType.APPLICATION_JSON))
          .orElse(false);
    }

    private Mono<? extends Map<String, Object>> readParameters(ClientResponse response) {
      ParameterizedTypeReference<Map<String, Object>> type = new ParameterizedTypeReference<>() {};
      BodyExtractor<Mono<Map<String, Object>>, ReactiveHttpInputMessage> extractor =
          BodyExtractors.toMono(type);
      return response.body(extractor);
    }

    private Map<String, Object> addTokenTypeIfNecessary(Map<String, Object> params) {
      Map<String, Object> newParams = new HashMap<>(params);
      Object tokenType = params.get("token_type");
      if (tokenType != null) {
        newParams.put("token_type", tokenType.toString().toLowerCase(Locale.ENGLISH));
      }
      Object token = params.get("token");
      if (token != null) {
        newParams.put("access_token", token);
      }
      Object expireIn = params.get("expire_in");
      if (expireIn != null) {
        try {
          int expiresIn = Integer.parseInt(expireIn.toString()) / 1000;
          newParams.put("expires_in", expiresIn);
        } catch (NumberFormatException e) {
          LOGGER.debug("expire_in value {} is not a valid number", e);
        }
      }
      return newParams;
    }

    private ClientResponse createNewResponse(
        ClientResponse originalResponse, Map<String, Object> params) {
      Publisher<Map<String, Object>> input = Mono.just(params);
      ResolvableType bodyType = ResolvableType.forInstance(params);
      HttpMessageEncoder<Object> encoder = new Jackson2JsonEncoder();
      MimeType elementType = MimeTypeUtils.APPLICATION_JSON;
      Map<String, Object> hints = Collections.emptyMap();
      DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;
      Flux<DataBuffer> newBody =
          encoder.encode(input, dataBufferFactory, bodyType, elementType, hints);
      return ClientResponse.create(originalResponse.statusCode(), originalResponse.strategies())
          .headers(headers -> headers.addAll(originalResponse.headers().asHttpHeaders()))
          .body(newBody)
          .build();
    }
  }
}
