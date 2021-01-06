package uk.gov.crowncommercial.esourcing.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class AuthConfiguration extends WebSecurityConfigurerAdapter {
  private static final String API_KEY_AUTH_HEADER_NAME = "API_KEY";

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_AUTH_HEADER_NAME);
    filter.setAuthenticationManager(new ApiKeyAuthManager());

    String whitelistedIP = System.getenv("whitelistedIP");
    String accessStr =
        whitelistedIP != null
            ? String.format(
                "isAuthenticated() and hasIpAddress(\"%s\")", System.getenv("whitelistedIP"))
            : "isAuthenticated()";

    http.authorizeRequests()
        .antMatchers("/Crown-Commercial/crown-commercial-service/v0_4/**")
        .access(accessStr)
        .anyRequest()
        .authenticated()
        .and()
        .addFilter(filter)
        .csrf()
        .disable()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }
}
