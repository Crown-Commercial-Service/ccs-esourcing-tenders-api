package uk.gov.crowncommercial.esourcing.integration.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Standard HTTP Servlet Filter that checks the HTTP requests source IP address is in a specified
 * list for simple allow list filtering.
 */
public class IpAddressFilter extends GenericFilterBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(IpAddressFilter.class);

  private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final List<IpAddressMatcher> ipAddressMatchers;

  private final List<String> includePaths;

  private final List<String> excludePaths;

  public IpAddressFilter(Set<String> ipAllowList, List<String> includePaths,
      List<String> excludePaths) {

    if (ipAllowList.isEmpty()) {
      LOGGER.warn(
          "No Allow List IP addresses have been specified so requests from all IP addresses will be accepted");
    } else {
      LOGGER.info("IP Allow List configuration: {}, include {}, exclude {}", ipAllowList,
          includePaths, excludePaths);
    }

    this.ipAddressMatchers = new ArrayList<>(ipAllowList.size());
    for (String ipAllow : ipAllowList) {
      ipAddressMatchers.add(new IpAddressMatcher(ipAllow));
    }
    this.includePaths = includePaths;
    this.excludePaths = excludePaths;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }
  }

  private void doHttpFilter(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    String path = getPath(request);

    if (excludePaths != null && !excludePaths.isEmpty() && path != null
        && matchPath(excludePaths, path)) {
      LOGGER.debug("Allowing request for path {} as this is defined in the exclusion list", path);
      chain.doFilter(request, response);
      return;
    }

    if (includePaths != null && !includePaths.isEmpty() && path != null
        && !matchPath(includePaths, path)) {
      LOGGER.debug("Allowing request for path {} as this is not defined in the inclusion list",
          path);
      chain.doFilter(request, response);
      return;
    }

    /*
     * Work out the IP address to validate, either from the X-Forwarded-For or the source IP address
     * from the request
     */
    String addressToValidate = null;

    /*
     * See
     * https://docs.cloud.service.gov.uk/deploying_services/route_services/#example-route-service-
     * to-add-ip-address-authentication for info on how GOV.UK PaaS sets the X-Forwarded-For header
     */
    String xForwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
    if (xForwardedFor != null) {
      addressToValidate =
          Arrays.stream(xForwardedFor.split(",")).map(String::trim).findFirst().orElse(null);
    }
    if (addressToValidate == null) {
      addressToValidate = request.getRemoteAddr();
    }

    LOGGER.debug("Remote address: {}, X-Forwarded-For: {}, Addresss to validate: {}",
        request.getRemoteAddr(), StringUtils.defaultString(xForwardedFor), addressToValidate);

    if (ipAddressMatchers.isEmpty()) {
      LOGGER.debug("Allowing request from address {} as no IP allow list is defined",
          addressToValidate);
      chain.doFilter(request, response);
      return;
    }

    if (matchesAddress(addressToValidate)) {
      LOGGER.debug("Allowing request from address {} as IP address is defined in the IP allow list",
          addressToValidate);
      chain.doFilter(request, response);
      return;
    }

    LOGGER.debug("Denying request from address {} as IP address is not in the allow list",
        addressToValidate);
    response.setStatus(HttpStatus.FORBIDDEN.value());
    return;
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

  protected String getPath(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    String pathInfo = request.getPathInfo();
    if (servletPath == null || servletPath.isEmpty()) {
      return pathInfo;
    } else {
      if (pathInfo == null || pathInfo.isEmpty()) {
        return servletPath;
      } else {
        return servletPath + pathInfo;
      }
    }
  }
}
