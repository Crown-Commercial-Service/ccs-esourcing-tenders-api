package uk.gov.crowncommercial.esourcing.auth;

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

  // TODO (pillingworth, 2020-01-08) make this configurable in the application.properties
  private static final String API_KEY_AUTH_HEADER_NAME = "x-api-key";

  @Value("${ccs.esourcing.tenders.apikey:}")
  private Set<String> apiKeys;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    ApiKeyAuthFilter apiKeyFilter = new ApiKeyAuthFilter(API_KEY_AUTH_HEADER_NAME);
    apiKeyFilter.setAuthenticationManager(new ApiKeyAuthManager(apiKeys));

    // @formatter:off
    http.addFilterBefore(ipAddressFilter(), AbstractPreAuthenticatedProcessingFilter.class)
        .addFilter(apiKeyFilter)
        .authorizeRequests()
          .antMatchers("/actuator/**").permitAll()
          .antMatchers("/openapi.yaml").permitAll()
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
    return new IpAddressFilter(); 
  }

  
}
