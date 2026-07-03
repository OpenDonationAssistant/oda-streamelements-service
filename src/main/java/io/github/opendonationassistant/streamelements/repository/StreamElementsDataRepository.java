package io.github.opendonationassistant.streamelements.repository;

import io.github.opendonationassistant.commons.logging.ODALogger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;

@Singleton
public class StreamElementsDataRepository {

  private ODALogger log = new ODALogger(this);
  private final Map<String, StreamElementsData> sessions;

  @Inject
  public StreamElementsDataRepository(
    Map<String, StreamElementsData> sessions
  ) {
    this.sessions = sessions;
  }

  public Optional<StreamElementsData> get(String recipientId) {
    return Optional.ofNullable(sessions.get(recipientId));
  }

  public void update(String recipientId, StreamElementsData data) {
    log.info(
      "Update session data",
      Map.of(
        "recipientId",
        recipientId,
        "data",
        data,
        "sessions",
        sessions.toString()
      )
    );
    sessions.put(recipientId, data);
  }
}
