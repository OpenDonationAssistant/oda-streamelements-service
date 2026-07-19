package io.github.opendonationassistant.streamelements.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelFollowEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class TwitchFollowEventHandler
  extends AbstractMessageHandler<TwitchChannelFollowEvent> {

  private final StreamElementsSessionRepository repository;

  @Inject
  public TwitchFollowEventHandler(
    ObjectMapper mapper,
    StreamElementsSessionRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(TwitchChannelFollowEvent event) throws IOException {
    final var recipientId = event.recipientId();
    if (recipientId == null) {
      return;
    }
    var username = event.username();
    if (username == null) {
      return;
    }
    repository.getSession(recipientId).setFollowLatest(username);
  }
}
