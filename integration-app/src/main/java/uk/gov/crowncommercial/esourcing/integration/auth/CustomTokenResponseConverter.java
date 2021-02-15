package uk.gov.crowncommercial.esourcing.integration.auth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

public class CustomTokenResponseConverter implements Converter<Map<String, String>, OAuth2AccessTokenResponse> {

  public static final String ACCESS_TOKEN = "token";
  public static final String EXPIRES_IN = "expire_in";
  private static final Set<String> TOKEN_RESPONSE_PARAMETER_NAMES =
      new HashSet<>(
          Arrays.asList(ACCESS_TOKEN, EXPIRES_IN, OAuth2ParameterNames.TOKEN_TYPE));

  @Override
  public OAuth2AccessTokenResponse convert(Map<String, String> tokenResponseParameters) {
    String accessToken = tokenResponseParameters.get(ACCESS_TOKEN);
    OAuth2AccessToken.TokenType accessTokenType = getAccessTokenType(tokenResponseParameters);
    long expiresIn = getExpiresIn(tokenResponseParameters);
    Map<String, Object> additionalParameters = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : tokenResponseParameters.entrySet()) {
      if (!TOKEN_RESPONSE_PARAMETER_NAMES.contains(entry.getKey())) {
        additionalParameters.put(entry.getKey(), entry.getValue());
      }
    }
    // @formatter:off
    return OAuth2AccessTokenResponse.withToken(accessToken)
        .tokenType(accessTokenType)
        .expiresIn(expiresIn)
        .additionalParameters(additionalParameters)
        .build();
    // @formatter:on
  }

  private OAuth2AccessToken.TokenType getAccessTokenType(
      Map<String, String> tokenResponseParameters) {
    if (OAuth2AccessToken.TokenType.BEARER
        .getValue()
        .equalsIgnoreCase(tokenResponseParameters.get(OAuth2ParameterNames.TOKEN_TYPE))) {
      return OAuth2AccessToken.TokenType.BEARER;
    }
    return null;
  }

  private long getExpiresIn(Map<String, String> tokenResponseParameters) {
    if (tokenResponseParameters.containsKey(OAuth2ParameterNames.EXPIRES_IN)) {
      try {
        return Long.parseLong(tokenResponseParameters.get(OAuth2ParameterNames.EXPIRES_IN));
      } catch (NumberFormatException ex) {
      }
    }
    return 0;
  }
}
