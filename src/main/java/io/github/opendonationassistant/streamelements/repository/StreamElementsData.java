package io.github.opendonationassistant.streamelements.repository;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record StreamElementsData(Tip tipGoal, Tip tipLatest) {
  public StreamElementsData withTipGoal(Tip tipGoal) {
    return new StreamElementsData(tipGoal, tipLatest);
  }
  public StreamElementsData withTipLatest(Tip tipLatest) {
    return new StreamElementsData(tipGoal, tipLatest);
  }

  @Serdeable
  public static record Tip(String name, Long amount) {}
}
