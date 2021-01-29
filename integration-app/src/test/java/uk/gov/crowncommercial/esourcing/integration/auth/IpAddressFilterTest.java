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
    
    assertThat(IpAddressFilter.getIpAddress(request)).isEqualTo("127.0.0.1");
  }

  @Test
  public void getIpAddress_oneAddressInXForwardedFor_expectRemoteAddress() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.8");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getIpAddress(request)).isEqualTo("127.0.0.1");
  }

  @Test
  public void getIpAddress_twoAddresesInXForwardedFor_expectRemoteAddress() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("123.123.123.123,10.0.0.8");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getIpAddress(request)).isEqualTo("123.123.123.123");
  }

  @Test
  public void getIpAddress_manyAddresesInXForwardedFor_expectRemoteAddress() {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1,1.2.3.4,123.123.123.123,10.0.0.8");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    assertThat(IpAddressFilter.getIpAddress(request)).isEqualTo("123.123.123.123");
  }
}
