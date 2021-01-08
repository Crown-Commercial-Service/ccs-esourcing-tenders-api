package uk.gov.crowncommercial.esourcing.auth;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class CustomIpAuthenticationProvider implements AuthenticationProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(CustomIpAuthenticationProvider.class);

  Set<String> allowListSet = new HashSet<String>();

  public CustomIpAuthenticationProvider() {
    allowListSet.add("0:0:0:0:0:0:0:12");
  }

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    WebAuthenticationDetails details = (WebAuthenticationDetails) auth.getDetails();
    String userIp = details.getRemoteAddress();

    logger.info("Request received from user IP address : {}}", userIp);

    if (!allowListSet.contains(userIp)) {
      throw new BadCredentialsException("Invalid IP Address");
    }

    auth.setAuthenticated(true);

    return auth;
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return false;
  }
}
