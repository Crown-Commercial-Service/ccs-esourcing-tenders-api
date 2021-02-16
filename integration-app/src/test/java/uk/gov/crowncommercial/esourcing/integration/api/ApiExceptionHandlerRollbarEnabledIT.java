package uk.gov.crowncommercial.esourcing.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.API_KEY_HEADER;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.CCS_API_BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.sender.Sender;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.crowncommercial.esourcing.integration.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.integration.app.ErrorResponse;
import uk.gov.crowncommercial.esourcing.integration.app.RollbarConfig;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiController;
import uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender;
import uk.gov.crowncommercial.esourcing.integration.service.TenderApiService;

@WebMvcTest(controllers = {TendersApiController.class})
@AutoConfigureMockMvc
@Import({AppConfiguration.class, RollbarConfig.class, IntegrationTestConfig.class})
@ActiveProfiles("integrationtest")
public class ApiExceptionHandlerRollbarEnabledIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TenderApiService tenderApiService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private Sender rollbarSender;

  @Autowired
  private Clock clock;

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.ip-allow-list", () -> "127.0.0.1");
    registry.add("ccs.esourcing.api-keys", () -> "integration-test-api-key");
    registry.add("rollbar.enabled", () -> "true");
    registry.add("rollbar.access-token", () -> "1298350192839123616203759102938");
    registry.add("rollbar.environment", () -> "integration_test");
    registry.add("info.app.version", () -> "12.34.56");
    registry.add("spring.security.oauth2.client.provider.jaggaer.token-uri", () -> "token-uri");
    registry.add("spring.security.oauth2.client.registration.jaggaer.client-id", () -> "client-id");
    registry.add("spring.security.oauth2.client.registration.jaggaer.client-secret", () -> "client-secret");
    registry.add("spring.security.oauth2.client.registration.jaggaer.authorization-grant-type", () -> "authorization-grant-type");
  }

  @Test
  public void salesforce_throwsNullPointerException_expectInternalServerErrorAndRollbarSend()
      throws Exception {

    /* mock the service call */
    when(tenderApiService.createProcurementCase(any(ProjectTender.class))).thenThrow(new NullPointerException(
        "Thrown as part of getTenderById_throwsNullPointerException_expectInternalServerError"));

    /* "call" the REST API */
    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.post(CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header(API_KEY_HEADER, "integration-test-api-key").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError()).andReturn();

    /* verify the REST API response */
    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(clock.instant()).status(500)
        .error("Internal Server Error").message("Unhandled exception")
        .path("/crown-commercial-service/ccs-esourcing-client/0.0.1-SNAPSHOT/tenders/ProcurementProjects/salesforce").build();
    String expected = objectMapper.writeValueAsString(errorResponse);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);

    /* check that the exception was also sent to rollbar, grabbing the payload sent as we do */
    ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
    verify(rollbarSender, times(1)).send(payloadCaptor.capture());
    Payload payload = payloadCaptor.getValue();
    assertThat(payload.getAccessToken()).isEqualTo("1298350192839123616203759102938");
    assertThat(payload.getData().getCodeVersion()).isEqualTo("12.34.56");
    assertThat(payload.getData().getEnvironment()).isEqualTo("integration_test");
    assertThat(payload.getData().getFramework()).isEqualTo("esourcing-integration-app");
    assertThat(payload.getData().getLanguage()).isEqualTo("java");
    assertThat(payload.getData().getLevel()).isEqualTo(Level.ERROR);
  }

  @Test
  public void getNoSuchFile_withApiKey_expectNotFoundAndNoRollbarSend() throws Exception {

    MvcResult mvcResult = mockMvc.perform(
        MockMvcRequestBuilders.get("/nosuchfile.txt").header(Constants.API_KEY_HEADER, "integration-test-api-key"))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();

    /* check that nothing was sent to rollbar */
    verify(rollbarSender, never()).send(any(Payload.class));
  }

}
