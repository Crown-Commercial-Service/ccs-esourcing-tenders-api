package uk.gov.crowncommercial.esourcing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.ccs.api.UserApiDelegate;
import uk.gov.crowncommercial.esourcing.ccs.model.User;

@Service
public class UserApiService implements UserApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(UserApiService.class);

  @Override
  public ResponseEntity<User> getUserByName(String username) {

    logger.info("Getting User with username : {}", username);

    User user = new User();

    user.setId(123L);
    user.setFirstName("Petros");
    user.setLastName("S");
    user.setUsername("Petros");
    user.setEmail("petors.stergioulas94@gmail.com");
    user.setPassword("secret");
    user.setPhone("+123 4567890");
    user.setUserStatus(0);

    return ResponseEntity.ok(user);
  }
}
