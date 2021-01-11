package uk.gov.crowncommercial.esourcing.api;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RunWith(SpringRunner.class)
@WebMvcTest
public class StaticFilesNoIpRestrictionsIT {

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.tenders.ipallowlist", () -> "");
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getOpenApiYaml_expectOk() throws Exception {
    
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/openapi.yaml"))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).startsWith("openapi: 3.0.0");
  }
  
  @Test
  public void getFavIcon_expectOk() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/favicon.ico"))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    assertThat(mvcResult.getResponse().getContentLength()).isEqualTo(6318);
  }

}
