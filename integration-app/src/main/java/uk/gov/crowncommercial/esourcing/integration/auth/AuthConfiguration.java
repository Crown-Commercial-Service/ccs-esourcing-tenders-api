package uk.gov.crowncommercial.esourcing.integration.auth;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

@Configuration
@EnableWebSecurity
public class AuthConfiguration extends WebSecurityConfigurerAdapter {

  @Value("${ccs.esourcing.tenders.apikeyheader:x-api-key}")
  private String apiKeyHeader;
  
  @Value("${ccs.esourcing.tenders.apikeys:}")
  private Set<String> apiKeys;
  
  @Value("${ccs.esourcing.tenders.ipallowlist:}")
  private Set<String> ipAllowList;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    ApiKeyAuthFilter apiKeyFilter = new ApiKeyAuthFilter(apiKeyHeader);
    apiKeyFilter.setAuthenticationManager(apiKeyAuthManager());

    // @formatter:off
    http.addFilterBefore(ipAddressFilter(), AbstractPreAuthenticatedProcessingFilter.class)
        .addFilter(apiKeyFilter)
        .authorizeRequests()
          .antMatchers("/actuator/**").permitAll()
          .antMatchers("/*/openapi.yaml").permitAll()
          .antMatchers("/favicon.ico").permitAll()
          .anyRequest().authenticated()
          .and()
        .csrf()
          .disable()
        .cors()
          .disable()
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
    return new IpAddressFilter(ipAllowList, null, Arrays.asList("/actuator/**"));
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
