package io.github.opendonationassistant.streamelements.repository;

import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

@Serdeable
public record StreamElementsData(
  Tip tipGoal,
  Tip tipLatest,
  @Nullable Follower followerLatest,
  @Nullable Subscriber subscriberLatest,
  @Nullable Raid raidLatest
) {
  public StreamElementsData withTipGoal(Tip tipGoal) {
    return new StreamElementsData(
      tipGoal,
      tipLatest,
      followerLatest,
      subscriberLatest,
      raidLatest
    );
  }
  public StreamElementsData withTipLatest(Tip tipLatest) {
    return new StreamElementsData(
      tipGoal,
      tipLatest,
      followerLatest,
      subscriberLatest,
      raidLatest
    );
  }
  public StreamElementsData withFollowerLatest(Follower followerLatest) {
    return new StreamElementsData(
      tipGoal,
      tipLatest,
      followerLatest,
      subscriberLatest,
      raidLatest
    );
  }
  public StreamElementsData withSubscriberLatest(Subscriber subscriberLatest) {
    return new StreamElementsData(
      tipGoal,
      tipLatest,
      followerLatest,
      subscriberLatest,
      raidLatest
    );
  }
  public StreamElementsData withRaidLatest(Raid raidLatest) {
    return new StreamElementsData(
      tipGoal,
      tipLatest,
      followerLatest,
      subscriberLatest,
      raidLatest
    );
  }

  @Serdeable
  public static record Tip(String name, Long amount) {}

  @Serdeable
  public static record Follower(String name) {}

  @Serdeable
  public static record Subscriber(
    String name,
    @Nullable String tier,
    @Nullable String message,
    @Nullable Integer cumulativeMonths,
    @Nullable Integer totalMonths,
    @Nullable Integer streakMonths
  ) {}

  @Serdeable
  public static record Raid(String name, @Nullable Integer viewerCount) {}
}
