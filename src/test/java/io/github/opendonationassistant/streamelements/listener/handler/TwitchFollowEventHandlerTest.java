package io.github.opendonationassistant.streamelements.listener.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.twitch.events.TwitchChannelFollowEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSession;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TwitchFollowEventHandlerTest {

  private final StreamElementsSessionRepository repository = mock(
    StreamElementsSessionRepository.class
  );
  private final StreamElementsSession session = mock(StreamElementsSession.class);
  private final TwitchFollowEventHandler handler = new TwitchFollowEventHandler(
    ObjectMapper.getDefault(),
    repository
  );

  @Test
  void handle_shouldSetFollowLatest() throws Exception {
    givenSession("recipient-1");

    handler.handle(follow("recipient-1", "follower"));

    verify(session).setFollowLatest("follower");
  }

  @Test
  void handle_shouldIgnoreMissingRecipientId() throws Exception {
    givenSession("recipient-1");

    handler.handle(follow(null, "follower"));

    verifyNoInteractions(session);
  }

  @Test
  void handle_shouldIgnoreMissingUsername() throws Exception {
    givenSession("recipient-1");

    handler.handle(follow("recipient-1", null));

    verifyNoInteractions(session);
  }

  @Test
  void handle_shouldIgnoreMissingSession() throws Exception {
    when(repository.getSession(any())).thenReturn(
      CompletableFuture.completedFuture(Optional.empty())
    );

    handler.handle(follow("recipient-1", "follower"));

    verifyNoInteractions(session);
  }

  @SuppressWarnings("NullAway")
  private static TwitchChannelFollowEvent follow(
    @Nullable String recipientId,
    @Nullable String username
  ) {
    return new TwitchChannelFollowEvent("id-1", recipientId, username, null);
  }

  private void givenSession(String recipientId) {
    when(repository.getSession(recipientId)).thenReturn(
      CompletableFuture.completedFuture(Optional.of(session))
    );
  }
}
