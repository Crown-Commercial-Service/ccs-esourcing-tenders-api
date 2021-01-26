package uk.gov.crowncommercial.esourcing.integration.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApplicationStartupCompleteListener
    implements ApplicationListener<ContextRefreshedEvent> {

  @Autowired
  private ActiveProfilesVerifier activeProfilesVerifier;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    
    activeProfilesVerifier.verify();
  }
}
