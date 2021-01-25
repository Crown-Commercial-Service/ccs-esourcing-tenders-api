package uk.gov.crowncommercial.esourcing.integration.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.API_KEY_HEADER;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.CCS_API_BASE_PATH;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.Sender;
import uk.gov.crowncommercial.esourcing.integration.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.integration.app.ErrorResponse;
import uk.gov.crowncommercial.esourcing.integration.app.RollbarConfig;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiController;
import uk.gov.crowncommercial.esourcing.integration.service.TenderApiService;

@WebMvcTest(controllers = {TendersApiController.class})
@AutoConfigureMockMvc
@Import({AppConfiguration.class, RollbarConfig.class, IntegrationTestConfig.class})
public class ApiExceptionHandlerRollbarDisabledIT {

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
    registry.add("ccs.esourcing.tenders.ipallowlist", () -> "127.0.0.1");
    registry.add("ccs.esourcing.tenders.apikeys", () -> "banana");
    registry.add("rollbar.enabled", () -> "false");
  }

  @Test
  public void getTenderById_throwsNullPointerException_expectInternalServerErrorAndRollbarSend()
      throws Exception {

    /* mock the service call */
    when(tenderApiService.getTenderById(anyLong())).thenThrow(new NullPointerException(
        "Thrown as part of getTenderById_throwsNullPointerException_expectInternalServerError"));

    /* "call" the REST API */
    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header(API_KEY_HEADER, "banana").contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError()).andReturn();

    /* verify the REST API response */
    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(clock.instant()).status(500)
        .error("Internal Server Error").message("Unhandled exception")
        .path("/Crown-Commercial/crown-commercial-service/v0_4/tenders/1").build();
    String expected = objectMapper.writeValueAsString(errorResponse);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);

    /* check that the exception was not sent to rollbar */
    verify(rollbarSender, never()).send(any(Payload.class));
  }
}
