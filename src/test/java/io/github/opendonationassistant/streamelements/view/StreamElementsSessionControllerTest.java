package io.github.opendonationassistant.streamelements.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.http.HttpStatus;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@MicronautTest(environments = "allinone")
@ExtendWith(InstancioExtension.class)
public class StreamElementsSessionControllerTest {

  @Inject
  StreamElementsSessionController controller;

  @Test
  @Disabled
  public void testGettingNewSession(@Given Authentication auth) {
    var session = controller.getSession(auth).join();
    assertEquals(HttpStatus.OK, session.getStatus());
  }
}
