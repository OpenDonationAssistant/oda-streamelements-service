package io.github.opendonationassistant.streamelements.listener;

import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;

@RabbitListener(executor = "event-listener")
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

    var session = repository.getSession(widget.ownerId()).join();
    if (session.isEmpty()) {
      return;
    }
    session.get().apply(widget);
  }
}
