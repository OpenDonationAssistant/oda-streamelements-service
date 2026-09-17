package io.github.opendonationassistant.streamelements.overlay;

import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Map;

/**
 * RPC client for oda-widgets-service `CreateWidgetRequestHandler`
 * (`widget.create-request` on the `rpc` exchange). Creates an empty widget and
 * replies with its id; config is applied afterwards via
 * {@code WidgetCommandSender}.
 */
@RabbitClient("rpc")
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
public interface WidgetCreateClient {
  String QUEUE_NAME = "widget.create-request";

  @Binding(QUEUE_NAME)
  CreatedWidget create(CreateWidgetRequest request);

  @Serdeable
  record CreateWidgetRequest(String type, String recipientId) {}

  /** Mirrors oda-widgets-service `WidgetDto`; only {@link #id()} is consumed. */
  @Serdeable
  record CreatedWidget(
    String id,
    String type,
    Integer sortOrder,
    String name,
    String ownerId,
    Map<String, Object> config,
    boolean enabled,
    boolean deleted,
    List<String> tags
  ) {}
}
