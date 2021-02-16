package uk.gov.crowncommercial.esourcing.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("integrationtest")
public class StaticFilesNoIpRestrictionsIT {

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.ip-allow-list", () -> "");
    registry.add("spring.security.oauth2.client.provider.jaggaer.token-uri", () -> "token-uri");
    registry.add("spring.security.oauth2.client.registration.jaggaer.client-id", () -> "client-id");
    registry.add("spring.security.oauth2.client.registration.jaggaer.client-secret", () -> "client-secret");
    registry.add("spring.security.oauth2.client.registration.jaggaer.authorization-grant-type", () -> "authorization-grant-type");
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getCCsOpenApiYaml_expectOk() throws Exception {
    
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/resources/ccs/openapi.yaml"))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).startsWith("openapi: 3.0.0");
  }
  
  @Test
  public void getJaggaerOpenApiYaml_expectOk() throws Exception {
    
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/resources/jaggaer/openapi.yaml"))
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
