package uk.gov.crowncommercial.esourcing.auth;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

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
}
