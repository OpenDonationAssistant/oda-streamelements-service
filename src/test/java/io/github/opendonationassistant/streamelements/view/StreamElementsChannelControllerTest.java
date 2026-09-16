package io.github.opendonationassistant.streamelements.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    sessions.createSession(channel);
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

  @Test
  public void testGettingChannelWithoutExistingSession() {
    var channel = "sessionless-channel";
    assertTrue(sessions.getSession(channel).join().isEmpty());
    var response = controller.getChannel(channel).join();
    assertEquals(HttpStatus.OK, response.getStatus());
    var body = (StreamElementsChannelView) response.getBody().get();
    assertEquals(channel, body.id());
    assertEquals(channel, body.username());
    assertEquals(channel, body.displayName());
    assertEquals(channel, body.alias());
    assertEquals(channel, body.providerId());
    assertEquals("twitch", body.provider());
    assertEquals(false, body.suspended());
    assertEquals(false, body.inactive());
    assertEquals(false, body.isPartner());
  }

}
