package io.github.opendonationassistant.streamelements.listener;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RabbitListener(executor = "config-listener")
public class WidgetChangesEventListener {

  public static final String QUEUE_NAME = "streamelements.config";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final Exchange BINDING = Exchange.Exchange(
    "changes.widgets",
    Map.of("*", WidgetChangesEventListener.QUEUE)
  );
  private final StreamElementsSessionRepository repository;

  @Inject
  public WidgetChangesEventListener(
    StreamElementsSessionRepository repository
  ) {
    this.repository = repository;
  }

  @Queue(QUEUE_NAME)
  public void handle(WidgetChangedEvent event) throws IOException {
    var widget = event.widget();
    if (widget == null) {
      return;
    }

    var widgetId = widget.id();
    if (widgetId == null) {
      return;
    }

    var config = widget.config();
    if (config == null) {
      return;
    }

    var properties = config.properties();
    if (properties == null) {
      return;
    }

    switch (widget.type()) {
      case "donationgoal" -> handleDonationGoal(event);
      default -> {}
    }
  }

  private void handleDonationGoal(WidgetChangedEvent event) throws IOException {
    event
      .widget()
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
            repository
              .getSession(event.widget().ownerId())
              .setDonationgoalState(new Amount(amount, 0, "RUB"));
          });
      });
  }
}
