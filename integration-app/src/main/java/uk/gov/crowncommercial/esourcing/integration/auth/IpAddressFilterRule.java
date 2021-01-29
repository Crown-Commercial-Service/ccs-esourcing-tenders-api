package uk.gov.crowncommercial.esourcing.integration.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.AntPathMatcher;

public class IpAddressFilterRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(IpAddressFilterRule.class);

  private final String name;
  
  private final List<IpAddressMatcher> ipAddressMatchers;

  private final List<String> includePaths;

  public enum Result {
    ALLOW, BLOCK, ABSTAIN;
  }

  public IpAddressFilterRule(String name, Set<String> ipAllowList, List<String> includePaths) {

    this.name = name;
    this.ipAddressMatchers = new ArrayList<>(ipAllowList.size());
    for (String ipAllow : ipAllowList) {
      ipAddressMatchers.add(new IpAddressMatcher(ipAllow));
    }
    this.includePaths = includePaths;
  }

  public Result filter(String path, String address) {

    if (includePaths != null && !includePaths.isEmpty() && path != null
        && !matchPath(includePaths, path)) {
      LOGGER.debug("Abstaining request for path {} as this is not defined in inclusion list for rule {}",
          path, name);
      return Result.ABSTAIN;
    }

    if (ipAddressMatchers.isEmpty()) {
      LOGGER.debug("Allowing request for path {} from address {} as no IP allow list is defined for rule {}",
          path, address, name);
      return Result.ALLOW;
    }

    if (matchesAddress(address)) {
      LOGGER.debug(
          "Allowing request for path {} from address {} as IP address is defined in the IP allow list for rule {}",
          path, address, name);
      return Result.ALLOW;
    }

    LOGGER.debug(
        "Denying request for path {} from address {} as IP address is not in the allow list for rule {}", path,
        address, name);
    return Result.BLOCK;
  }

  private boolean matchesAddress(String address) {

    for (IpAddressMatcher ipAddressMatcher : ipAddressMatchers) {
      if (ipAddressMatcher.matches(address)) {
        return true;
      }
    }
    return false;
  }

  /**
   * ANT style path matching against a list of path patterns
   * 
   * @param paths the path patterns to match against, if null or empty then no match will ever be
   *        made
   * @param path the path to match, if null no match will be made
   * @return true if the path matches any of the patterns
   */
  protected static final boolean matchPath(List<String> paths, String path) {

    if (paths == null || paths.isEmpty() || path == null) {
      return false;
    }

    AntPathMatcher matcher = new AntPathMatcher();
    for (String pattern : paths) {
      if (matcher.isPattern(pattern)) {
        if (matcher.match(pattern, path)) {
          return true;
        }
      } else { // exact match
        if (pattern.equals(path)) {
          return true;
        }
      }
    }
    return false;
  }
}
