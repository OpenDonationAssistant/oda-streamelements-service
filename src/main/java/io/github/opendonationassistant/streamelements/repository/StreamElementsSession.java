package io.github.opendonationassistant.streamelements.repository;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import io.github.opendonationassistant.streamelements.WidgetFacade.Detail;
import io.github.opendonationassistant.streamelements.WidgetFacade.Event;
import io.github.opendonationassistant.streamelements.WidgetFacade.Payload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
      .map(goal -> (List<Map<String, Object>>) goal.value())
      .ifPresent(properties -> {
        properties
          .stream()
          .filter(item -> Boolean.TRUE.equals(item.get("default")))
          .findFirst()
          .map(item -> (Map<String, Object>) item.get("accumulatedAmount"))
          .map(amount -> (Integer) amount.get("major"))
          .ifPresent(amount -> {
            setDonationgoalState(new Amount(amount, 0, "RUB"));
          });
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
