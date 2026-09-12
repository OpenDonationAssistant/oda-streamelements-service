package io.github.opendonationassistant.streamelements.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData.Follower;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData.Raid;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData.Subscriber;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData.Tip;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@Serdeable
public record StreamElementsSessionView(Channel channel, ViewSession session) {
  @Serdeable
  public static record Channel(String username) {}

  @Serdeable
  public static record ViewSession(ViewData data) {
    public static ViewSession of(
      io.github.opendonationassistant.streamelements.repository.StreamElementsSession session
    ) {
      return new ViewSession(
        new ViewData(
          Optional.ofNullable(ViewTip.of(session.data().tipLatest())).orElseGet(
            () -> new ViewTip("", 0L)
          ),
          Optional.ofNullable(ViewTip.of(session.data().tipGoal())).orElseGet(
            () -> new ViewTip("", 0L)
          ),
          ViewFollower.of(session.data().followerLatest()),
          ViewSubscriber.of(session.data().subscriberLatest()),
          ViewRaid.of(session.data().raidLatest())
        )
      );
    }
  }

  @Serdeable
  public static record ViewData(
    @JsonProperty("tip-latest") ViewTip tipLatest,
    @JsonProperty("tip-goal") ViewTip tipGoal,
    @Nullable @JsonProperty("follower-latest") ViewFollower followerLatest,
    @Nullable @JsonProperty(
      "subscriber-latest"
    ) ViewSubscriber subscriberLatest,
    @Nullable @JsonProperty("raid-latest") ViewRaid raidLatest
  ) {}

  @Serdeable
  public static record ViewTip(String name, Long amount) {
    public static @Nullable ViewTip of(@Nullable Tip tip) {
      return Optional.ofNullable(tip)
        .map(it -> new ViewTip(it.name(), it.amount()))
        .orElse(null);
    }
  }

  @Serdeable
  public static record ViewFollower(String name) {
    public static @Nullable ViewFollower of(@Nullable Follower follower) {
      return Optional.ofNullable(follower)
        .map(it -> new ViewFollower(it.name()))
        .orElse(null);
    }
  }

  @Serdeable
  public static record ViewSubscriber(
    String name,
    @Nullable String tier,
    @Nullable String message,
    @Nullable Integer cumulativeMonths,
    @Nullable Integer totalMonths,
    @Nullable Integer streakMonths
  ) {
    public static @Nullable ViewSubscriber of(@Nullable Subscriber subscriber) {
      return Optional.ofNullable(subscriber)
        .map(it ->
          new ViewSubscriber(
            it.name(),
            it.tier(),
            it.message(),
            it.cumulativeMonths(),
            it.totalMonths(),
            it.streakMonths()
          )
        )
        .orElse(null);
    }
  }

  @Serdeable
  public static record ViewRaid(String name, @Nullable Integer viewerCount) {
    public static @Nullable ViewRaid of(@Nullable Raid raid) {
      return Optional.ofNullable(raid)
        .map(it -> new ViewRaid(it.name(), it.viewerCount()))
        .orElse(null);
    }
  }
}
