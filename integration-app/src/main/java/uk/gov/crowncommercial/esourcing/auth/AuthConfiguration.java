package uk.gov.crowncommercial.esourcing.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class AuthConfiguration extends WebSecurityConfigurerAdapter {

  private static final String API_KEY_AUTH_HEADER_NAME = "x-api-key";

  @Value("${ccs.esourceing.tenders.apikey}")
  private String apikey;

  @Value("${ccs.esourceing.tenders.allowlist}")
  private String allowList;

  @Autowired private CustomIpAuthenticationProvider authenticationProvider;

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(authenticationProvider);
  }

  @Bean
  AuthenticationManager apiKeyAuthManager() {
    return new ApiKeyAuthManager(apikey);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_AUTH_HEADER_NAME);
    filter.setAuthenticationManager(apiKeyAuthManager());

    http.authorizeRequests()
        .antMatchers("/actuator/**")
        .permitAll()
        .antMatchers("/Crown-Commercial/crown-commercial-service/v0_4/**")
        .authenticated()
        .and()
        .addFilter(filter)
        .csrf()
        .disable()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }
}
