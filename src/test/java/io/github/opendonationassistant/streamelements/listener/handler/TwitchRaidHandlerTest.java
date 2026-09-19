package io.github.opendonationassistant.streamelements.listener.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.twitch.events.TwitchChannelRaidEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSession;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TwitchRaidHandlerTest {

  private final StreamElementsSessionRepository repository = mock(
    StreamElementsSessionRepository.class
  );
  private final StreamElementsSession session = mock(StreamElementsSession.class);
  private final TwitchRaidHandler handler = new TwitchRaidHandler(
    ObjectMapper.getDefault(),
    repository
  );

  @Test
  void handle_shouldSetRaidLatest() throws Exception {
    givenSession("recipient-1");

    handler.handle(raid("raider", 42));

    verify(session).setRaidLatest(new StreamElementsData.Raid("raider", 42));
  }

  @Test
  void handle_shouldDefaultViewerCountToNull() throws Exception {
    givenSession("recipient-1");

    handler.handle(raid("raider", null));

    verify(session).setRaidLatest(new StreamElementsData.Raid("raider", null));
  }

  @Test
  void handle_shouldIgnoreMissingFromChannelName() throws Exception {
    givenSession("recipient-1");

    handler.handle(raid(null, 42));

    verifyNoInteractions(session);
  }

  @Test
  void handle_shouldIgnoreMissingSession() throws Exception {
    when(repository.getSession(any())).thenReturn(
      CompletableFuture.completedFuture(Optional.empty())
    );

    handler.handle(raid("raider", 42));

    verifyNoInteractions(session);
  }

  @SuppressWarnings("NullAway")
  private static TwitchChannelRaidEvent raid(
    @Nullable String fromChannelName,
    @Nullable Integer viewerCount
  ) {
    return new TwitchChannelRaidEvent(
      "id-1",
      "recipient-1",
      fromChannelName,
      viewerCount
    );
  }

  private void givenSession(String recipientId) {
    when(repository.getSession(recipientId)).thenReturn(
      CompletableFuture.completedFuture(Optional.of(session))
    );
  }
}
