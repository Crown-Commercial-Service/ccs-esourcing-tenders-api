package uk.gov.crowncommercial.esourcing.integration.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;

public class ActiveProfilesVerifier {

  private static final Set<String> ENVIRONMENT_PROFILES = Collections.unmodifiableSet(
      new HashSet<String>(Arrays.asList("local", "dev", "sandbox", "test", "uat")));

  private final boolean enabled;

  @Value("${spring.profiles.active:}")
  private Set<String> activeProfiles;


  public ActiveProfilesVerifier() {
    this(true);
  }

  public ActiveProfilesVerifier(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Check the "environment" profiles against a defined set and ensure one and only one is defined.
   * 
   * @param activeProfiles the active profiles defined
   * @throws IllegalStateException if 0 or >1 are defined
   */
  public void verify() throws IllegalStateException {
    if (enabled) {
      doVerify(activeProfiles);
    }
  }

  public static void doVerify(Set<String> activeProfiles) throws IllegalStateException {

    Set<String> intersect = new HashSet<>(ENVIRONMENT_PROFILES);
    intersect.retainAll(activeProfiles);

    if (intersect.size() == 0) {
      // no environmental profile set - we need one
      throw new IllegalStateException(
          "No environment profile has been defined. Set spring.profiles.active to one of "
              + String.join(",", ENVIRONMENT_PROFILES));
    } else if (intersect.size() == 1) {
      // just one - this is what we want
    } else {
      // more than one environmental profile set - we need just one
      throw new IllegalStateException("Multiple environment profiles has been defined "
          + String.join(",", intersect) + ". Set spring.profiles.active to only one of "
          + String.join(",", ENVIRONMENT_PROFILES));
    }
  }
}
