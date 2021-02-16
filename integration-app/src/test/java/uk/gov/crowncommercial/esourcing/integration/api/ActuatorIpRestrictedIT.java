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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.crowncommercial.esourcing.integration.app.AppConfiguration;
import uk.gov.crowncommercial.esourcing.integration.app.RollbarConfig;

@WebMvcTest
@AutoConfigureMockMvc
@Import({AppConfiguration.class, RollbarConfig.class, IntegrationTestConfig.class, ActuatorIpRestrictedIT.MockActuator.class})
@ActiveProfiles("integrationtest")
public class ActuatorIpRestrictedIT {

  @DynamicPropertySource
  public static void setDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("ccs.esourcing.ipallow-list", () -> "192.168.0.0");
    registry.add("ccs.esourcing.actuator-ipallow-list", () -> "10.0.0.0/24");
    registry.add("ccs.esourcing.api-keys", () -> "integration-test-api-key");
    registry.add("spring.security.oauth2.client.provider.jaggaer.token-uri", () -> "token-uri");
    registry.add("spring.security.oauth2.client.registration.jaggaer.client-id", () -> "client-id");
    registry.add("spring.security.oauth2.client.registration.jaggaer.client-secret", () -> "client-secret");
    registry.add("spring.security.oauth2.client.registration.jaggaer.authorization-grant-type", () -> "authorization-grant-type");
  }
  
  @RestController
  public static class MockActuator {
    @GetMapping("/actuator/info")
    public String info() {
      return "";
    }
  }

  @Autowired
  private MockMvc mockMvc;
  
  @Test
  public void getActuatorsInfo_validInternalIp_expectOk() throws Exception {

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get("/actuator/info")
            .header("X-Forwarded-For", "10.0.0.1"))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  public void getActuatorsInfo_invalidInternalIp_expectOk() throws Exception {

    MvcResult mvcResult = mockMvc
        .perform(MockMvcRequestBuilders.get("/actuator/info")
            .header("X-Forwarded-For", "10.0.1.0"))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }
}
