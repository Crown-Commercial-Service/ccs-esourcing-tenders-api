package uk.gov.crowncommercial.esourcing.integration.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.API_KEY_HEADER;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.CCS_API_BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
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
import uk.gov.crowncommercial.esourcing.integration.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.integration.app.ErrorResponse;
import uk.gov.crowncommercial.esourcing.integration.service.TenderApiService;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiController;

@WebMvcTest(controllers = {TendersApiController.class})
@AutoConfigureMockMvc
@Import({AppConfiguration.class})
public class ApiExceptionHandlerIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TenderApiService tenderApiService;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private Clock clock;

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.tenders.ipallowlist", () -> "127.0.0.1");
    registry.add("ccs.esourcing.tenders.apikeys", () -> "banana");
  }

  @Test
  public void getTenderById_throwsNullPointerException_expectInternalServerError()
      throws Exception {

    when(tenderApiService.getTenderById(anyLong())).thenThrow(new NullPointerException());
    when(clock.instant()).thenReturn(Instant.ofEpochSecond(1610454603L));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header(API_KEY_HEADER, "banana").contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError()).andReturn();

    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(clock.instant()).status(500)
        .error("Internal Server Error").message("Unhandled exception")
        .path("/Crown-Commercial/crown-commercial-service/v0_4/tenders/1").build();
    String expected = objectMapper.writeValueAsString(errorResponse);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }
}
