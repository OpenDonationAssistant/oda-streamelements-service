package io.github.opendonationassistant.streamelements.repository;

import io.github.opendonationassistant.streamelements.WidgetFacade;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StreamElementsSessionRepository {

  private final StreamElementsDataRepository sessions;
  private final WidgetFacade facade;

  @Inject
  public StreamElementsSessionRepository(
    StreamElementsDataRepository sessions,
    WidgetFacade facade
  ) {
    this.sessions = sessions;
    this.facade = facade;
  }

  public StreamElementsSession getSession(String recipientId) {
    return sessions
      .get(recipientId)
      .map(data -> convert(recipientId, data))
      .orElseGet(() -> startSession(recipientId));
  }

  private StreamElementsSession convert(
    String recipientId,
    StreamElementsData data
  ) {
    return new StreamElementsSession(recipientId, data, sessions, facade);
  }

  public StreamElementsSession startSession(String recipientId) {
    var data = new StreamElementsData(
      new StreamElementsData.Tip("", 0L),
      new StreamElementsData.Tip("", 0L)
    );
    sessions.update(recipientId, data);
    return convert(recipientId, data);
  }
}
