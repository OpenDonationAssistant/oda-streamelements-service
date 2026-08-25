package io.github.opendonationassistant.streamelements;

import io.github.opendonationassistant.events.widget.Widget;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import org.jspecify.annotations.Nullable;

@RabbitClient("rpc")
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
public interface WidgetConfigClient {
  String QUEUE_NAME = "widget.config-request";

  @Binding(QUEUE_NAME)
  List<Widget> request(WidgetConfigRequest request);

  @Serdeable
  record WidgetConfigRequest(
    @Nullable String widgetId,
    @Nullable String widgetType
  ) {}
}
