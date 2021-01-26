package uk.gov.crowncommercial.esourcing.integration.app;

import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  InfoAppConfigurationProperties infoAppCfgProps;

  @Bean
  RollbarConfigurationProperties rollbarConfigurationProperties() {
    return new RollbarConfigurationProperties();
  }

  @Bean
  public Rollbar rollbar(Supplier<Sender> senderSupplier) {

    RollbarConfigurationProperties cfgProps = rollbarConfigurationProperties();

    ConfigBuilder builder = RollbarSpringConfigBuilder.withAccessToken(cfgProps.getAccessToken())
        .enabled(cfgProps.isEnabled()).sender(senderSupplier.get());

    if (cfgProps.getEndpoint() != null) {
      builder.endpoint(cfgProps.getEndpoint());
    }

    if (cfgProps.getEnvironment() != null) {
      builder.environment(cfgProps.getEnvironment());
    }

    /* framework defaults to app name (if set) or the framework value if set */
    if (infoAppCfgProps.getName() != null) {
      builder.framework(infoAppCfgProps.getName());
    }
    if (cfgProps.getFramework() != null) {
      builder.framework(cfgProps.getFramework());
    }

    if (infoAppCfgProps.getVersion() != null) {
      builder.codeVersion(infoAppCfgProps.getVersion());
    }

    return new Rollbar(builder.build());
  }

  @Bean
  public Supplier<Sender> senderSupplier() {
    /* provide this as a supplier so can return null */
    return () -> null;
  }

}
