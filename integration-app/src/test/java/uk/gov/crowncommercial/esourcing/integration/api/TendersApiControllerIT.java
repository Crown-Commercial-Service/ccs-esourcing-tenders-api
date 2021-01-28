package uk.gov.crowncommercial.esourcing.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.API_KEY_HEADER;
import static uk.gov.crowncommercial.esourcing.integration.api.Constants.CCS_API_BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.crowncommercial.esourcing.integration.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.integration.app.RollbarConfig;
import uk.gov.crowncommercial.esourcing.integration.service.TenderApiService;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiController;
import uk.gov.crowncommercial.esourcing.integration.server.model.Tender;

@WebMvcTest(controllers = {TendersApiController.class})
@AutoConfigureMockMvc
@Import({AppConfiguration.class, RollbarConfig.class, IntegrationTestConfig.class})
@ActiveProfiles("integrationtest")
public class TendersApiControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TenderApiService tenderApiService;
  
  @Autowired
  private ObjectMapper objectMapper;

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.ip-allow-list", () -> "127.0.0.1");
    registry.add("ccs.esourcing.api-keys", () -> "integration-test-api-key");
  }

  @Test
  public void getTenderById_expectOk() throws Exception {

    Tender tender = new Tender().id(1L).description("description").status(2);
    when(tenderApiService.getTenderById(anyLong()))
        .thenReturn(new ResponseEntity<Tender>(tender, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header(API_KEY_HEADER, "integration-test-api-key").contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    String expected = objectMapper.writeValueAsString(tender);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }

  @Test
  public void getTenderById_noApiKey_expectForbidden() throws Exception {

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

}
