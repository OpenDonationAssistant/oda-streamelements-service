package io.github.opendonationassistant.streamelements.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

@Singleton
public class HistoryItemEventHandler
  extends AbstractMessageHandler<HistoryItemEvent> {

  private final StreamElementsSessionRepository repository;

  @Inject
  public HistoryItemEventHandler(
    ObjectMapper mapper,
    StreamElementsSessionRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(HistoryItemEvent event) throws IOException {
    final var originId = event.originId();
    if (originId == null) {
      return;
    }
    if (!"payment".equals(event.type())) {
      return;
    }
    var nickname = event.nickname();
    if (nickname == null) {
      return;
    }
    var amount = event.amount();
    if (amount == null) {
      return;
    }
    var message = event.message();
    repository
      .getSession(event.recipientId())
      .setTipsLatest(nickname, amount, Optional.ofNullable(message).orElse(""));
  }
}
