package uk.gov.crowncommercial.esourcing.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.gov.crowncommercial.esourcing.integration.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.integration.app.RollbarConfig;

@WebMvcTest
@AutoConfigureMockMvc
@Import({AppConfiguration.class, RollbarConfig.class, IntegrationTestConfig.class})
public class StaticFilesIpRestrictedIT {

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.tenders.ipallowlist", () -> "123.456.789.123");
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getOpenApiYaml_expectForbidden() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/openapi.yaml"))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();
    
    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void getFavIcon_expectForbidden() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/favicon.ico"))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();
    
    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

}
