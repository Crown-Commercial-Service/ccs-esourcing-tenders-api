package uk.gov.crowncommercial.esourcing.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
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
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiController;
import uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender;
import uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender200Response;
import uk.gov.crowncommercial.esourcing.integration.service.EmailService;
import uk.gov.crowncommercial.esourcing.integration.service.TenderApiService;

@WebMvcTest(controllers = {TendersApiController.class})
@AutoConfigureMockMvc
@Import({AppConfiguration.class, RollbarConfig.class, IntegrationTestConfig.class})
@ActiveProfiles("integrationtest")
public class TendersApiControllerIpRestrictedIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TenderApiService tenderApiService;

  @MockBean
  private EmailService emailService;

  @Autowired
  private ObjectMapper objectMapper;

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.ip-allow-list", () -> "192.168.0.1/30");
    registry.add("ccs.esourcing.api-keys", () -> "integration-test-api-key");
  }

  ClassLoader classLoader = getClass().getClassLoader();
  InputStream inputStream = classLoader.getResourceAsStream("test-data/valid-request-body.json");
  String requestBody;
  {
    assert inputStream != null;
    requestBody = new BufferedReader(new InputStreamReader(inputStream))
        .lines().collect(Collectors.joining("\n"));
  }

  @Test
  public void salesforce_expectForbidden() throws Exception {

    ProjectTender200Response response = new ProjectTender200Response().tenderReferenceCode("trc").rfxReferenceCode("rfc");
    when(tenderApiService.createCase(any(ProjectTender.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.post(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header(Constants.API_KEY_HEADER, "integration-test-api-key").contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void salesforce_noApiKey_expectForbidden() throws Exception {

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.post(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void salesforce_withValidXForwardedForHeader_expectOk() throws Exception {

    ProjectTender200Response response = new ProjectTender200Response().tenderReferenceCode("trc").rfxReferenceCode("rfc");
    when(tenderApiService.createCase(any(ProjectTender.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.put(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header("X-Forwarded-For", "192.168.0.1").header(Constants.API_KEY_HEADER, "integration-test-api-key")
            .contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    String expected = objectMapper.writeValueAsString(response);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }

  @Test
  public void salesforce_withValidLowerLimitMultipleXForwardedForHeader_expectOk() throws Exception {

    ProjectTender200Response response = new ProjectTender200Response().tenderReferenceCode("trc").rfxReferenceCode("rfc");
    when(tenderApiService.createCase(any(ProjectTender.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.put(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header("X-Forwarded-For", "192.168.0.0, 10.0.0.1").header(Constants.API_KEY_HEADER, "integration-test-api-key")
            .contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    String expected = objectMapper.writeValueAsString(response);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }

  @Test
  public void salesforce_withValidUpperLimitMultipleXForwardedForHeader_expectOk() throws Exception {

    ProjectTender200Response response = new ProjectTender200Response().tenderReferenceCode("trc").rfxReferenceCode("rfc");
    when(tenderApiService.createCase(any(ProjectTender.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.put(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header("X-Forwarded-For", "192.168.0.3, 10.0.0.1").header(Constants.API_KEY_HEADER, "integration-test-api-key")
            .contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    String expected = objectMapper.writeValueAsString(response);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }

  @Test
  public void salesforce_withInvalidXForwardedForHeader_expectForbidden() throws Exception {

    ProjectTender200Response response = new ProjectTender200Response().tenderReferenceCode("trc").rfxReferenceCode("rfc");
    when(tenderApiService.createCase(any(ProjectTender.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.put(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header("X-Forwarded-For", "192.168.0.4").header(Constants.API_KEY_HEADER, "integration-test-api-key")
            .contentType(MediaType.APPLICATION_JSON).content("requestBody"))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void salesforce_withInvalidMultipleXForwardedForHeader_expectForbidden() throws Exception {

    ProjectTender200Response response = new ProjectTender200Response().tenderReferenceCode("trc").rfxReferenceCode("rfc");
    when(tenderApiService.createCase(any(ProjectTender.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.put(Constants.CCS_API_BASE_PATH + "/tenders/ProcurementProjects/salesforce")
            .header("X-Forwarded-For", "192.168.0.4, 10.0.0.1").header(Constants.API_KEY_HEADER, "integration-test-api-key")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }
}
