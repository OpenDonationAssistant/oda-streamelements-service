package io.github.opendonationassistant.streamelements.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelRaidEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class TwitchRaidHandler
  extends AbstractMessageHandler<TwitchChannelRaidEvent> {

  private final StreamElementsSessionRepository repository;

  @Inject
  public TwitchRaidHandler(
    ObjectMapper mapper,
    StreamElementsSessionRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(TwitchChannelRaidEvent event) throws IOException {
    final var recipientId = event.recipientId();
    if (recipientId == null) {
      return;
    }
    var fromChannelName = event.fromChannelName();
    if (fromChannelName == null) {
      return;
    }
    var session = repository.getSession(recipientId).join();
    if (session.isEmpty()) {
      return;
    }
    session
      .get()
      .setRaidLatest(
        new StreamElementsData.Raid(fromChannelName, event.viewerCount())
      );
  }
}
