package io.github.opendonationassistant.streamelements.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
public class StreamElementsChannelControllerTest {

  @Inject
  StreamElementsChannelController controller;

  @Inject
  StreamElementsSessionRepository sessions;

  @Test
  public void testGettingExistingChannel() {
    var channel = "test-channel";
    sessions.startSession(channel);
    var response = controller.getChannel(channel).join();
    assertEquals(HttpStatus.OK, response.getStatus());
    var body = (StreamElementsChannelView) response.getBody().get();
    assertEquals("test-channel", body.id());
    assertEquals("test-channel", body.username());
    assertEquals("test-channel", body.displayName());
    assertEquals("test-channel", body.alias());
    assertEquals("test-channel", body.providerId());
    assertEquals("twitch", body.provider());
    assertEquals(false, body.suspended());
    assertEquals(false, body.inactive());
    assertEquals(false, body.isPartner());
  }

  // @Test
  // public void testGettingMissingChannel() {
  //   var response = controller.getChannel("missing-channel").join();
  //   assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
  // }
}
