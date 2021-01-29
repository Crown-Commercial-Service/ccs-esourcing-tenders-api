package uk.gov.crowncommercial.esourcing.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class IpAddressFilterTest {

  public IpAddressFilterTest() {

  }

  @Test
  public void matchPath_typical() {

    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator"), "/actuator")).isTrue();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator/*"), "/actuator")).isFalse();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator/*"), "/actuator/")).isTrue();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator/**"), "/actuator")).isTrue();

    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator"), "/actuator/health")).isFalse();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator/*"), "/actuator/health"))
        .isTrue();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator/*"), "/actuator/health/"))
        .isFalse();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator/**"), "/actuator/health"))
        .isTrue();

    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator"), "/actuator/health/component"))
        .isFalse();
    assertThat(
        IpAddressFilter.matchPath(Arrays.asList("/actuator/*"), "/actuator/health/component"))
            .isFalse();
    assertThat(
        IpAddressFilter.matchPath(Arrays.asList("/actuator/*"), "/actuator/health/component/"))
            .isFalse();
    assertThat(
        IpAddressFilter.matchPath(Arrays.asList("/actuator/**"), "/actuator/health/component"))
            .isTrue();
  }

  @Test
  public void matchPath_edgeCases() {

    assertThat(IpAddressFilter.matchPath(null, "/actuator")).isFalse();
    assertThat(IpAddressFilter.matchPath(Arrays.asList(), "/actuator")).isFalse();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator"), "")).isFalse();
    assertThat(IpAddressFilter.matchPath(Arrays.asList("/actuator"), null)).isFalse();
  }
  
  @Test
  public void getIpAddress_noXForwardedFor_expectRemoteAddress() {
    
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getXForwardedForIpAddress(request)).isNull();
  }

  @Test
  public void getIpAddress_oneAddressInXForwardedFor_expectXForwardedFor() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.8");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getXForwardedForIpAddress(request)).isEqualTo("10.0.0.8");
  }

  @Test
  public void getIpAddress_twoAddresesInXForwardedFor_expectSecondLastXForwardedFor() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("123.123.123.123, 10.0.0.8");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getXForwardedForIpAddress(request)).isEqualTo("123.123.123.123");
  }

  @Test
  public void getIpAddress_manyAddresesInXForwardedFor_expectSecondLastXForwardedFor() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 1.2.3.4, 123.123.123.123, 10.0.0.8");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getXForwardedForIpAddress(request)).isEqualTo("123.123.123.123");
  }
  
  @Test
  public void getIpAddress_swaggerHubExample_expectSecondLastXForwardedFor() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("109.148.168.12, 64.252.152.159, 10.101.20.22, 3.223.162.99, 10.0.0.239");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getXForwardedForIpAddress(request)).isEqualTo("3.223.162.99");
  }
}
