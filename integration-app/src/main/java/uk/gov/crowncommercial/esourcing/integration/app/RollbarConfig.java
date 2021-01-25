package uk.gov.crowncommercial.esourcing.integration.app;

import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.spring.webmvc.RollbarSpringConfigBuilder;

/**
 * See https://docs.rollbar.com/docs/spring
 */
@Configuration
@ComponentScan({"com.rollbar.spring"})
public class RollbarConfig {

  @Value("${rollbar.enabled:false}")
  private boolean enabled;

  @Value("${rollbar.accesstoken:#{null}}")
  private String accessToken;

  @Value("${rollbar.environment:#{null}}")
  private String environment;

  @Value("${rollbar.framework:#{null}}")
  private String framework;

  @Value("${rollbar.endpoint:#{null}}")
  private String endpoint;

  @Value("${info.app.name:#{null}}")
  private String infoAppName;

  @Value("${info.app.version:#{null}}")
  private String infoAppVersion;

  @Bean
  public Rollbar rollbar(Supplier<Sender> senderSupplier) {

    ConfigBuilder builder = RollbarSpringConfigBuilder.withAccessToken(accessToken).enabled(enabled)
        .sender(senderSupplier.get());

    if (accessToken != null) {
      builder.accessToken(accessToken);
    }

    if (endpoint != null) {
      builder.endpoint(endpoint);
    }

    if (environment != null) {
      builder.environment(environment);
    }

    /* framework defaults to app name (if set) or the framework value if set */
    if (infoAppName != null) {
      builder.framework(infoAppName);
    }
    if (framework != null) {
      builder.framework(framework);
    }

    if (infoAppVersion != null) {
      builder.codeVersion(infoAppVersion);
    }

    return new Rollbar(builder.build());
  }

  @Bean
  public Supplier<Sender> senderSupplier() {
    /* provide this as a supplier so can return null */
    return () -> null;
  }

}
