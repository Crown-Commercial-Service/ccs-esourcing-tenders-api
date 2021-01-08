package uk.gov.crowncommercial.esourcing.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

public class ApiKeyAuthManager implements AuthenticationManager {

  private final String apikey;

  public ApiKeyAuthManager(String apikey) {
    this.apikey = apikey;
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    String principal = (String) authentication.getPrincipal();

    if (apikey != null && !apikey.equals(principal)) {
      throw new BadCredentialsException("The API key was not found or not the expected value.");
    } else {
      authentication.setAuthenticated(true);
      return authentication;
    }
  }
}
