package io.github.opendonationassistant.streamelements.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionMessageEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class TwitchSubscriptionHandler
  extends AbstractMessageHandler<TwitchChannelSubscriptionMessageEvent> {

  private final StreamElementsSessionRepository repository;

  @Inject
  public TwitchSubscriptionHandler(
    ObjectMapper mapper,
    StreamElementsSessionRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(TwitchChannelSubscriptionMessageEvent event) throws IOException {
    final var recipientId = event.recipientId();
    if (recipientId == null) {
      return;
    }
    var username = event.username();
    if (username == null) {
      return;
    }
    var message = event.message();
    var session = repository.getSession(recipientId).join();
    if (session.isEmpty()) {
      return;
    }
    session
      .get()
      .setSubscriberLatest(
        new StreamElementsData.Subscriber(
          username,
          event.tier(),
          message == null ? null : message.text(),
          event.cumulativeMonths(),
          event.totalMonths(),
          event.streakMonths()
        )
      );
  }
}