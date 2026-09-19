package io.github.opendonationassistant.streamelements.listener.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionMessageEvent;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionMessageEvent.Message;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSession;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TwitchSubscriptionHandlerTest {

  private final StreamElementsSessionRepository repository = mock(
    StreamElementsSessionRepository.class
  );
  private final StreamElementsSession session = mock(StreamElementsSession.class);
  private final TwitchSubscriptionHandler handler =
    new TwitchSubscriptionHandler(ObjectMapper.getDefault(), repository);

  @Test
  void handle_shouldSetSubscriberLatestWithMessage() throws Exception {
    givenSession("recipient-1");

    handler.handle(subscription("sub", message("hello")));

    verify(session).setSubscriberLatest(
      new StreamElementsData.Subscriber("sub", "1000", "hello", 3, 6, 2)
    );
  }

  @Test
  void handle_shouldDefaultMissingMessageToNull() throws Exception {
    givenSession("recipient-1");

    handler.handle(subscription("sub", null));

    verify(session).setSubscriberLatest(
      new StreamElementsData.Subscriber("sub", "1000", null, 3, 6, 2)
    );
  }

  @Test
  void handle_shouldIgnoreMissingUsername() throws Exception {
    givenSession("recipient-1");

    handler.handle(subscription(null, message("hello")));

    verifyNoInteractions(session);
  }

  @Test
  void handle_shouldIgnoreMissingSession() throws Exception {
    when(repository.getSession(any())).thenReturn(
      CompletableFuture.completedFuture(Optional.empty())
    );

    handler.handle(subscription("sub", message("hello")));

    verifyNoInteractions(session);
  }

  private static Message message(String text) {
    return new Message(text, List.of());
  }

  @SuppressWarnings("NullAway")
  private static TwitchChannelSubscriptionMessageEvent subscription(
    @Nullable String username,
    @Nullable Message message
  ) {
    return new TwitchChannelSubscriptionMessageEvent(
      "id-1",
      "recipient-1",
      username,
      "1000",
      message,
      3,
      6,
      2
    );
  }

  private void givenSession(String recipientId) {
    when(repository.getSession(recipientId)).thenReturn(
      CompletableFuture.completedFuture(Optional.of(session))
    );
  }
}
