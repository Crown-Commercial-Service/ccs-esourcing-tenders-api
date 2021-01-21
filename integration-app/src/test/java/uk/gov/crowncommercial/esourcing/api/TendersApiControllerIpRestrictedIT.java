package uk.gov.crowncommercial.esourcing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static uk.gov.crowncommercial.esourcing.api.Constants.API_KEY_HEADER;
import static uk.gov.crowncommercial.esourcing.api.Constants.CCS_API_BASE_PATH;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.crowncommercial.esourcing.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.ccs.api.TendersApiController;
import uk.gov.crowncommercial.esourcing.ccs.model.Tender;
import uk.gov.crowncommercial.esourcing.service.TenderApiService;

@WebMvcTest(controllers = {TendersApiController.class})
@AutoConfigureMockMvc
@Import({AppConfiguration.class})
public class TendersApiControllerIpRestrictedIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TenderApiService tenderApiService;
  
  @Autowired
  private ObjectMapper objectMapper;

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.tenders.ipallowlist", () -> "123.456.789.123");
    registry.add("ccs.esourcing.tenders.apikeys", () -> "banana");
  }

  @Test
  public void getTenderById_expectForbidden() throws Exception {

    Tender tender = new Tender().id(1L).description("description").status(2);
    when(tenderApiService.getTenderById(anyLong()))
        .thenReturn(new ResponseEntity<Tender>(tender, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header(API_KEY_HEADER, "banana").contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void getTenderById_noApiKey_expectForbidden() throws Exception {

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void getTenderById_withValidXForwardedForHeader_expectOk() throws Exception {

    Tender tender = new Tender().id(1L).description("description").status(2);
    when(tenderApiService.getTenderById(anyLong()))
        .thenReturn(new ResponseEntity<Tender>(tender, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header("X-Forwarded-For", "123.456.789.123").header(API_KEY_HEADER, "banana")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    String expected = objectMapper.writeValueAsString(tender);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }

  @Test
  public void getTenderById_withValidMultipleXForwardedForHeader_expectOk() throws Exception {

    Tender tender = new Tender().id(1L).description("description").status(2);
    when(tenderApiService.getTenderById(anyLong()))
        .thenReturn(new ResponseEntity<Tender>(tender, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header("X-Forwarded-For", "123.456.789.123, 10.0.0.1").header(API_KEY_HEADER, "banana")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    String expected = objectMapper.writeValueAsString(tender);
    JSONAssert.assertEquals(expected, mvcResult.getResponse().getContentAsString(), false);
  }
  
  @Test
  public void getTenderById_withInvalidXForwardedForHeader_expectForbidden() throws Exception {

    Tender tender = new Tender().id(1L).description("description").status(2);
    when(tenderApiService.getTenderById(anyLong()))
        .thenReturn(new ResponseEntity<Tender>(tender, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header("X-Forwarded-For", "111.222.333.444").header(API_KEY_HEADER, "banana")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void getTenderById_withInvalidMultipleXForwardedForHeader_expectForbidden() throws Exception {

    Tender tender = new Tender().id(1L).description("description").status(2);
    when(tenderApiService.getTenderById(anyLong()))
        .thenReturn(new ResponseEntity<Tender>(tender, HttpStatus.OK));

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get(CCS_API_BASE_PATH + "/tenders/1")
            .header("X-Forwarded-For", "111.222.333.444, 10.0.0.1").header(API_KEY_HEADER, "banana")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }
}
