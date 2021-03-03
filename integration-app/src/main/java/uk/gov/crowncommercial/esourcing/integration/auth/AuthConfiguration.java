package uk.gov.crowncommercial.esourcing.integration.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class AuthConfiguration extends WebSecurityConfigurerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthConfiguration.class);

  @Value("${ccs.esourcing.api-key-header:x-api-key}")
  private String apiKeyHeader;

  @Value("${ccs.esourcing.api-keys:}")
  private Set<String> apiKeys;

  @Value("${ccs.esourcing.ip-allow-list:}")
  private Set<String> ipAllowList;

  @Value("${ccs.esourcing.actuator-ipallow-list:}")
  private Set<String> actuatorIpAllowList;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    LOGGER.info("API Key header name of '{}', used with API keys - {}.", apiKeyHeader, apiKeys);
    ApiKeyAuthFilter apiKeyFilter = new ApiKeyAuthFilter(apiKeyHeader);
    apiKeyFilter.setAuthenticationManager(apiKeyAuthManager());

    // @formatter:off
    http.addFilter(apiKeyFilter)
        .authorizeRequests()
          .antMatchers("/actuator/**").permitAll()
          .antMatchers("/*/*/openapi.yaml").permitAll()
          .antMatchers("/favicon.ico").permitAll()
          .anyRequest().authenticated()
          .and()
        .csrf()
          .disable()
        // see https://stackoverflow.com/a/45685747/210445
        .cors().configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues())
          .and()
        .formLogin()
          .disable()
        .httpBasic()
          .disable()
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    // @formatter:on
  }

  @Bean
  IpAddressFilter ipAddressFilter() {
    List<IpAddressFilterRule> rules = new ArrayList<>();

    List<String> actuatorPaths = Arrays.asList("/actuator/**");
    if (!actuatorIpAllowList.isEmpty()) {
      LOGGER.info("Configuring filter with Actuator IP Allow List for paths {}, ip addresses {}",
          String.join(",", actuatorPaths), String.join(",", actuatorIpAllowList));
      IpAddressFilterRule internalIpAddressRule =
          new IpAddressFilterRule("actuator", actuatorIpAllowList, actuatorPaths);
      rules.add(internalIpAddressRule);
    } else {
      LOGGER.warn(
          "No Actuator IP Allow List has been defined so requests from all IP addresses will be accepted for paths {}",
          actuatorPaths);
    }

    List<String> allPaths = Collections.emptyList();
    if (!ipAllowList.isEmpty()) {
      LOGGER.info("Configuring filter with IP Allow List for paths {}, ip addresses {}",
          String.join(",", allPaths), String.join(",", ipAllowList));
      IpAddressFilterRule ipAddressRule = new IpAddressFilterRule("default", ipAllowList, allPaths);
      rules.add(ipAddressRule);
    } else {
      LOGGER.warn(
          "No IP Allow List has been defined so requests from all IP addresses will be accepted for paths {}",
          allPaths);
    }
    return new IpAddressFilter(rules);
  }

  /**
   * Creating our own AuthenticationManager in the Spring application context prevents Spring Boots
   * UserDetailsServiceAutoConfiguration from creating and configuraing an
   * InMemoryUserDetailsService.
   * 
   * @return the API Key AuthenticationManager
   */
  @Bean
  ApiKeyAuthManager apiKeyAuthManager() {
    return new ApiKeyAuthManager(apiKeys);
  }
}
