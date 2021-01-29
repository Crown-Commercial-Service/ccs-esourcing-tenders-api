package uk.gov.crowncommercial.esourcing.integration.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Standard HTTP Servlet Filter that checks the HTTP requests source IP address is in a specified
 * list for simple allow list filtering.
 */
public class IpAddressFilter extends GenericFilterBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(IpAddressFilter.class);

  private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final List<IpAddressFilterRule> ipAddressFilterRules;

  public IpAddressFilter(List<IpAddressFilterRule> ipAddressFilterRules) {

    this.ipAddressFilterRules = ipAddressFilterRules;
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
    String ipAddress = getIpAddress(request);


    if (ipAddressFilterRules.isEmpty()) {
      LOGGER.debug(
          "Allowing request for path {} from address {} as no IP filtering list is defined", path,
          ipAddress);
      chain.doFilter(request, response);
      return;
    }

    if (matchesAddress(path, ipAddress)) {
      LOGGER.debug(
          "Allowing request for path {} from address {} as IP address is defined in the IP allow list",
          path, ipAddress);
      chain.doFilter(request, response);
      return;
    }

    LOGGER.debug(
        "Denying request for path {} from address {} as IP address is not in the allow list", path,
        ipAddress);
    response.setStatus(HttpStatus.FORBIDDEN.value());
    return;
  }

  private boolean matchesAddress(String path, String address) {

    for (IpAddressFilterRule rule : ipAddressFilterRules) {
      IpAddressFilterRule.Result result = rule.filter(path, address);
      switch (result) {
        case ALLOW:
          return true;
        case BLOCK:
          return false;
        case ABSTAIN:
          break;
      }
    }

    // no rules blocked so assume it is ok
    return true;
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

  /**
   * Get the path from the request; includes the servlet name and the path info
   * 
   * @param request the HTTP servlet request
   * @return the path; may return empty string, should never return null
   */
  protected static String getPath(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    String pathInfo = request.getPathInfo();
    if (servletPath == null || servletPath.isEmpty()) {
      return StringUtils.trimToEmpty(pathInfo);
    } else {
      if (pathInfo == null || pathInfo.isEmpty()) {
        return servletPath;
      } else {
        return servletPath + pathInfo;
      }
    }
  }

  /**
   * Get the ip address to check from the request; uses X_FORWARDED_FOR_HEADER or remote address
   * 
   * @param request the HTTP servlet request
   * @return the ip address to check, should never return null
   */
  protected static final String getIpAddress(HttpServletRequest request) {
    /*
     * Work out the IP address to validate, either from the X-Forwarded-For or the source IP address
     * from the request
     */
    String ipAddress = null;

    /*
     * See
     * https://docs.cloud.service.gov.uk/deploying_services/route_services/#example-route-service-
     * to-add-ip-address-authentication for info on how GOV.UK PaaS sets the X-Forwarded-For header
     * but basically we need the second from last in the list.
     */
    String xForwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
    if (xForwardedFor != null) {
      List<String> ipAddresses =
          Arrays.stream(xForwardedFor.split(",")).map(String::trim).collect(Collectors.toList());
      if (ipAddresses.size() == 1) {
        ipAddress = ipAddresses.get(ipAddresses.size() - 1);
      } else if (ipAddresses.size() > 1) {
        ipAddress = ipAddresses.get(ipAddresses.size() - 2);
      }
    }
    if (ipAddress == null) {
      ipAddress = request.getRemoteAddr();
    }

    LOGGER.debug("Remote address: {}, X-Forwarded-For: {}, Addresss to validate: {}",
        request.getRemoteAddr(), StringUtils.defaultString(xForwardedFor), ipAddress);

    return ipAddress;
  }
}
