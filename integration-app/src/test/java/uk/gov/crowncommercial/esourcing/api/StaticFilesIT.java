package uk.gov.crowncommercial.esourcing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.crowncommercial.esourcing.api.Constants.API_KEY_HEADER;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.crowncommercial.esourcing.mvc.MvcConfiguration;

@WebMvcTest
@AutoConfigureMockMvc
@Import({MvcConfiguration.class})
public class StaticFilesIT {

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.tenders.ipallowlist", () -> "127.0.0.1");
    registry.add("ccs.esourcing.tenders.apikeys", () -> "banana");
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getOpenApiYaml_noApiKey_expectOk() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/openapi.yaml"))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).startsWith("openapi: 3.0.0");
  }

  @Test
  public void getFavIcon_noApiKey_expectOk() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/favicon.ico"))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    assertThat(mvcResult.getResponse().getContentLength()).isEqualTo(6318);
  }

  @Test
  public void getNoSuchFile_noApiKey_expectForbidden() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/nosuchfile.txt"))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void getNoSuchFile_withApiKey_expectNotFound() throws Exception {

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get("/nosuchfile.txt").header(API_KEY_HEADER, "banana"))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }
}
