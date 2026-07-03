package io.github.opendonationassistant.streamelements.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.instancio.junit.Given;
import org.junit.Test;

import io.micronaut.http.HttpStatus;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

@MicronautTest
public class StreamElementsSessionControllerTest {

  @Inject
  StreamElementsSessionController controller;


  @Test
  public void testGettingNewSession(@Given Authentication auth) {
    var session = controller.getSession(auth).join();
    assertEquals(HttpStatus.OK, session.getStatus());
  }
  
}
