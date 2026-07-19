package io.github.opendonationassistant.streamelements.repository;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record StreamElementsData(
  Tip tipGoal,
  Tip tipLatest,
  Follower followerLatest
) {
  public StreamElementsData withTipGoal(Tip tipGoal) {
    return new StreamElementsData(tipGoal, tipLatest, followerLatest);
  }
  public StreamElementsData withTipLatest(Tip tipLatest) {
    return new StreamElementsData(tipGoal, tipLatest, followerLatest);
  }
  public StreamElementsData withFollowerLatest(Follower followerLatest) {
    return new StreamElementsData(tipGoal, tipLatest, followerLatest);
  }

  @Serdeable
  public static record Tip(String name, Long amount) {}
  
  @Serdeable
  public static record Follower(String name) {}
}
