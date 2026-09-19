package io.github.opendonationassistant.streamelements.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.opendonationassistant.events.MessageProcessor;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import org.junit.jupiter.api.Test;

// TODO add intgration test with MicronautTest
class EventsListenerTest {

  private final MessageProcessor processor = mock(MessageProcessor.class);
  private final EventsListener listener = new EventsListener(processor);

  @Test
  void listen_shouldDelegateToMessageProcessor() throws Exception {
    var payload = new byte[] { 1, 2, 3 };
    var ack = mock(RabbitAcknowledgement.class);

    listener.listen("event.TwitchChannelFollowEvent", payload, ack);

    verify(processor).process("event.TwitchChannelFollowEvent", payload, ack);
  }
}
