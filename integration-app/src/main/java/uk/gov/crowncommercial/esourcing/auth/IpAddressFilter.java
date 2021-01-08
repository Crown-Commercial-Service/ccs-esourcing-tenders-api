package uk.gov.crowncommercial.esourcing.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Standard HTTP Servlet Filter that checks the HTTP requests source IP address is in a specified
 * list for simple allow list gating.
 */
public class IpAddressFilter extends GenericFilterBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(IpAddressFilter.class);

  @Value("${ccs.esourcing.tenders.ipallowlist:}")
  private Set<String> ipAllowList;

  public IpAddressFilter() {}

  @PostConstruct
  public void init() {
    if (ipAllowList.isEmpty()) {
      LOGGER.warn(
          "No Allow List IP addresses have been specified so requests from all IP addresses will be accepted");
    } else {
      LOGGER.info("IP Allow List configuration: {}", ipAllowList);
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
  }

  private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    // TODO (pillingworth, 2020-01-08) make the paths checked by this filter configurable or use
    // proper servlet filters and map to a path properly - probably means not adding in as a auth
    // filter
    if (request.getServletPath().startsWith("/actuator")) {

      LOGGER.debug("Allowing request on path {}", request.getServletPath());

    } else {

      String remoteAddr = request.getRemoteAddr();
      String addressToValidate = remoteAddr;

      /*
       * See
       * https://docs.cloud.service.gov.uk/deploying_services/route_services/#example-route-service-
       * to-add-ip-address-authentication for info on how GOV.UK PaaS sets the X-Forwarded-For
       * header
       */
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null) {
        String[] ipAddresses =
            Arrays.stream(xForwardedFor.split(",")).map(String::trim).toArray(String[]::new);
        if (ipAddresses.length > 0) {
          addressToValidate = ipAddresses[0];
        }
      }

      LOGGER.debug("Remote address: {}, X-Forwarded-For: {}, Addresss to validate: {}",
          addressToValidate, StringUtils.defaultString(xForwardedFor), addressToValidate);

      if (ipAllowList.isEmpty()) {
        LOGGER.debug("Allowing request from {} as no IP allow list is defined", addressToValidate);
      } else if (ipAllowList.contains(addressToValidate)) {
        LOGGER.debug("Allowing request from {} as IP address is defined in the IP allow list",
            addressToValidate);
      } else {
        LOGGER.debug("Denying request from {} as IP address is not in the allow list",
            addressToValidate);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return;
      }
    }

    chain.doFilter(request, response);
  }
}
