package io.github.opendonationassistant.streamelements.repository;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StreamElementsDataRepository {

  private final Map<String, StreamElementsData> sessions;

  @Inject
  public StreamElementsDataRepository(Map<String, StreamElementsData> sessions) {
    this.sessions = sessions;
  }

  public Optional<StreamElementsData> get(String recipientId) {
    return Optional.ofNullable(sessions.get(recipientId));
  }

  public void update(String recipientId, StreamElementsData data) {
    sessions.put(recipientId, data);
  }
  
}
