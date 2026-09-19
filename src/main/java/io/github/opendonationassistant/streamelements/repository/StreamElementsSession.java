package io.github.opendonationassistant.streamelements.repository;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import io.github.opendonationassistant.streamelements.WidgetFacade.Detail;
import io.github.opendonationassistant.streamelements.WidgetFacade.Event;
import io.github.opendonationassistant.streamelements.WidgetFacade.Payload;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class StreamElementsSession {

  private final String recipientId;
  private final StreamElementsDataRepository repository;
  private StreamElementsData data;
  private WidgetFacade facade;

  public StreamElementsSession(
    String recipientId,
    StreamElementsData data,
    StreamElementsDataRepository repository,
    WidgetFacade facade
  ) {
    this.recipientId = recipientId;
    this.data = data;
    this.repository = repository;
    this.facade = facade;
  }

  public StreamElementsData data() {
    return data;
  }

  public void setDonationgoalState(Amount collected) {
    this.data = data.withTipGoal(
      new StreamElementsData.Tip("", collected.getMajor().longValue())
    );
    this.save();
  }

  /** Fills session state from a widget config, same as the widget changes listener. */
  public void apply(Widget widget) {
    var config = widget.config();
    if (config == null) {
      return;
    }
    var properties = config.properties();
    if (properties == null) {
      return;
    }
    switch (widget.type()) {
      case "donationgoal" -> applyDonationGoal(widget);
      default -> {}
    }
  }

  private void applyDonationGoal(Widget widget) {
    widget
      .config()
      .getProperty("goal")
      .stream()
      .map(property -> property.value())
      .flatMap(value ->
        value instanceof List<?> list ? list.stream() : Stream.of()
      )
      .flatMap(listItem ->
        listItem instanceof Map<?, ?> map ? Stream.of(map) : Stream.of()
      )
      .filter(goal ->
        Optional.ofNullable(goal.get("mode"))
          .map(String::valueOf)
          .filter("default"::equalsIgnoreCase)
          .isPresent()
      )
      .forEach(goal -> {
        if (
          goal.get("accumulatedAmount") instanceof Map<?, ?> amount &&
          amount.get("major") instanceof Number major
        ) {
          setDonationgoalState(new Amount(major.intValue(), 0, "RUB"));
        }
      });
  }

  public CompletableFuture<Void> setFollowLatest(String name) {
    this.data = data.withFollowerLatest(new StreamElementsData.Follower(name));
    this.save();
    return facade.sendEvent(
      recipientId,
      new Event(new Detail("follower-latest", Payload.empty().withName(name)))
    );
  }

  public CompletableFuture<Void> setSubscriberLatest(
    StreamElementsData.Subscriber subscriber
  ) {
    this.data = data.withSubscriberLatest(subscriber);
    this.save();
    return facade.sendEvent(
      recipientId,
      new Event(
        new Detail(
          "subscriber-latest",
          Payload.empty()
            .withName(subscriber.name())
            .withMessage(Optional.ofNullable(subscriber.message()).orElse(""))
        )
      )
    );
  }

  public CompletableFuture<Void> setRaidLatest(StreamElementsData.Raid raid) {
    this.data = data.withRaidLatest(raid);
    this.save();
    return facade.sendEvent(
      recipientId,
      new Event(
        new Detail(
          "raid-latest",
          Payload.empty()
            .withName(raid.name())
            .withAmount(
              Optional.ofNullable(raid.viewerCount())
                .map(Integer::longValue)
                .orElse(0L)
            )
        )
      )
    );
  }

  public CompletableFuture<Void> setTipsLatest(
    String name,
    Amount tip,
    String message
  ) {
    this.data = data.withTipLatest(
      new StreamElementsData.Tip(name, tip.getMajor().longValue())
    );
    this.save();
    return facade.sendEvent(
      recipientId,
      new Event(
        new Detail(
          "tip-latest",
          Payload.empty()
            .withName(name)
            .withAmount(tip.getMajor().longValue())
            .withMessage(message)
        )
      )
    );
  }

  public void save() {
    repository.update(recipientId, data);
  }
}
