package uk.gov.crowncommercial.esourcing.integration.auth;

import java.util.Set;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

public class ApiKeyAuthManager implements AuthenticationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyAuthManager.class);

  private final Set<String> apiKeys;

  public ApiKeyAuthManager(@NotNull Set<String> apiKeys) {

    if (apiKeys.isEmpty()) {
      LOGGER.warn("No API Keys have been defined so no requests will be authenticated");
    } else {
      LOGGER.info("{} API Keys configured", apiKeys.size());
    }

    this.apiKeys = apiKeys;
  }

  @Override
  public Authentication authenticate(Authentication authentication) {

    String principal = (String) authentication.getPrincipal();

    if (!apiKeys.contains(principal)) {
      LOGGER.debug("Not authenticating request as API Key is not in defined list");
      throw new BadCredentialsException("API key not found");
    }

    LOGGER.debug("Authenticating request as API Key is in defined list");
    authentication.setAuthenticated(true);

    return authentication;
  }
}
