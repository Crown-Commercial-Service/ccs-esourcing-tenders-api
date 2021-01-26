package uk.gov.crowncommercial.esourcing.integration.app;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ApiClient;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ProjectsApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxApi;

@Configuration
public class AppConfiguration {

  // TODO Work out how to get the bearer token
  @Value("${jaggaer.bearer.token:}")
  private String bearerToken;

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

  @Scope("prototype")
  public ApiClient apiClient() {

    ApiClient apiClient = new ApiClient();

    apiClient.setBearerToken(bearerToken);

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
}
