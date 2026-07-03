package io.github.opendonationassistant.streamelements.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record StreamElementsSessionView(Channel channel, Session session) {
  @Serdeable
  public static record Channel(String username) {}

  @Serdeable
  public static record Session(Data data) {
    public static Session of(
      io.github.opendonationassistant.streamelements.repository.StreamElementsSession session
    ) {
      return new Session(
        new Data(
          Tip.of(session.data().tipLatest()),
          Tip.of(session.data().tipGoal())
        )
      );
    }
  }

  @Serdeable
  public static record Data(
    @JsonProperty("tip-latest") Tip tipLatest,
    @JsonProperty("tip-goal") Tip tipGoal
  ) {}

  @Serdeable
  public static record Tip(String name, Long amount) {
    public static Tip of(
      io.github.opendonationassistant.streamelements.repository.StreamElementsData.Tip tip
    ) {
      return new Tip(tip.name(), tip.amount());
    }
  }
}
