package uk.gov.crowncommercial.esourcing.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

public class ApiKeyAuthManager implements AuthenticationManager {

  private final String key;

  public ApiKeyAuthManager() {
    this.key =
        System.getenv("apikey") != null
            ? System.getenv("apikey")
            : "84b0d964-6047-428c-a41f-7df98ccdc6b6";
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    String principal = (String) authentication.getPrincipal();

    if (key != null && !key.equals(principal)) {
      throw new BadCredentialsException("The API key was not found or not the expected value.");
    } else {
      authentication.setAuthenticated(true);
      return authentication;
    }
  }
}
