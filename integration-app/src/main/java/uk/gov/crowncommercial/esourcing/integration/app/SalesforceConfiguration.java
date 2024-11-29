package uk.gov.crowncommercial.esourcing.integration.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
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
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactivePasswordTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.crowncommercial.esourcing.salesforce.client.ApiClient;
import uk.gov.crowncommercial.esourcing.salesforce.client.RFC3339DateFormat;
import uk.gov.crowncommercial.esourcing.salesforce.client.RfxStatusListApi;

@Configuration
public class SalesforceConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceConfiguration.class);

  @Value("${ccs.esourcing.salesforce.client-url}")
  private String salesforceClientUrl;

  @Value("${ccs.esourcing.salesforce.oauth2.username}")
  private String username;

  @Value("${ccs.esourcing.salesforce.oauth2.password}")
  private String password;

  // Expires in defaulted to 4 hours as confirmed with Bright Gen
  @Value("${ccs.esourcing.salesforce.oauth2.token.expires-in:14400}")
  private Long tokenExpiresIn;

  @Value("${spring.security.oauth2.client.registration.salesforce.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.salesforce.client-secret}")
  private String clientSecret;

  private final ObjectMapper clientObjectMapper;

  public SalesforceConfiguration(ObjectMapper clientObjectMapper) {
    this.clientObjectMapper = clientObjectMapper;
  }

  @Bean
  ReactiveClientRegistrationRepository salesforceClientRegistrationRepository(
      @Value("${spring.security.oauth2.client.provider.salesforce.token-uri}") String tokenUri,
      @Value("${spring.security.oauth2.client.registration.salesforce.client-id}") String clientId,
      @Value("${spring.security.oauth2.client.registration.salesforce.client-secret}")
          String clientSecret) {
    ClientRegistration salesforceClientRegistration =
        ClientRegistration.withRegistrationId("salesforce")
            .tokenUri(tokenUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.PASSWORD)
            .build();
    return new InMemoryReactiveClientRegistrationRepository(salesforceClientRegistration);
  }

  @Bean
  public ApiClient salesforceApiClient(
      ReactiveClientRegistrationRepository salesforceClientRegistrationRepository) {

    ReactiveOAuth2AuthorizedClientService authorizedClientService =
        new InMemoryReactiveOAuth2AuthorizedClientService(salesforceClientRegistrationRepository);

    AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            salesforceClientRegistrationRepository, authorizedClientService);

    WebClientReactivePasswordTokenResponseClient webClientReactivePasswordTokenResponseClient =
        new WebClientReactivePasswordTokenResponseClient();

    WebClient accessTokenWebClient =
        WebClient.builder()
            .filter(requestPasswordTokenRequestProcessor())
            .filter(responsePasswordTokenRequestProcessor())
            .build();

    webClientReactivePasswordTokenResponseClient.setWebClient(accessTokenWebClient);

    ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
            .password(
                consumer ->
                    consumer.accessTokenResponseClient(
                        webClientReactivePasswordTokenResponseClient))
            .build();

    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    authorizedClientManager.setContextAttributesMapper(
        oAuth2AuthorizeRequest ->
            Mono.just(
                Map.of(
                    OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username,
                    OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password)));

    /*
     * This code is largely duplicated from the autogenerated Salesforce API code
     */
    DateFormat dateFormat = new RFC3339DateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                clientDefaultCodecsConfigurer -> {
                  clientDefaultCodecsConfigurer
                      .defaultCodecs()
                      .jackson2JsonEncoder(
                          new Jackson2JsonEncoder(clientObjectMapper, MediaType.APPLICATION_JSON));
                  clientDefaultCodecsConfigurer
                      .defaultCodecs()
                      .jackson2JsonDecoder(
                          new Jackson2JsonDecoder(clientObjectMapper, MediaType.APPLICATION_JSON));
                })
            .build();

    /*
     * Add the oauth filter to the webclient which will be used during api authentication
     */
    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

    oauth.setDefaultClientRegistrationId("salesforce");
    WebClient.Builder webClientBuilder = WebClient.builder().exchangeStrategies(strategies);
    WebClient webClient = webClientBuilder.filter(oauth).build();
    ApiClient apiClient = new ApiClient(webClient, clientObjectMapper, dateFormat);

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

  private ExchangeFilterFunction requestPasswordTokenRequestProcessor() {
    return ExchangeFilterFunction.ofRequestProcessor(
        request ->
            Mono.just(
                ClientRequest.from(request)
                    .body(
                        ((BodyInserters.FormInserter<String>) request.body())
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret))
                    .build()));
  }

  private ExchangeFilterFunction responsePasswordTokenRequestProcessor() {
    return ExchangeFilterFunction.ofResponseProcessor(
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
    Object accessToken = params.get("access_token");
    Object expiresIn = params.get("expires_in");
    if (accessToken != null && expiresIn == null) {
      newParams.put("expires_in", tokenExpiresIn);
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
