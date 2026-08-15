package io.github.opendonationassistant.streamelements.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record StreamElementsChannelView(
  Profile profile,
  @JsonProperty("_id") String id,
  @JsonProperty("isPartner") boolean isPartner,
  @JsonProperty("providerId") String providerId,
  @JsonProperty("displayName") String displayName,
  String username,
  String avatar,
  boolean suspended,
  String provider,
  @JsonProperty("broadcasterType") String broadcasterType,
  String alias,
  boolean inactive
) {
  public static StreamElementsChannelView of(String channel) {
    return new StreamElementsChannelView(
      new Profile("", ""),
      channel,
      false,
      channel,
      channel,
      channel,
      "",
      false,
      "twitch",
      "",
      channel,
      false
    );
  }

  @Serdeable
  public static record Profile(String title, String headerImage) {}
}
