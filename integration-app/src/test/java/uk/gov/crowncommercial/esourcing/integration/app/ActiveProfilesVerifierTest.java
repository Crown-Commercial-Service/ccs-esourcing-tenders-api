package uk.gov.crowncommercial.esourcing.integration.app;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class ActiveProfilesVerifierTest {

  @Test
  public void verify_testNoActiveProfiles_expectException() {

    assertThrows(IllegalStateException.class, () -> {
      ActiveProfilesVerifier.doVerify(new HashSet<>());
    });
  }

  @Test
  public void verify_testNoEnvironmentActiveProfiles_expectException() {

    assertThrows(IllegalStateException.class, () -> {
      ActiveProfilesVerifier
          .doVerify(new HashSet<String>(Arrays.asList("default", "postgres", "es7")));
    });
  }

  @Test
  public void verify_testOneEnvironmentActiveProfile_expectOkay() {

    ActiveProfilesVerifier.doVerify(new HashSet<String>(Arrays.asList("dev", "postgres", "es7")));
  }

  @Test
  public void verify_testTwoEnvironmentActiveProfiles_expectException() {

    assertThrows(IllegalStateException.class, () -> {
      ActiveProfilesVerifier
          .doVerify(new HashSet<String>(Arrays.asList("dev", "uat", "postgres", "es7")));
    });
  }
}
