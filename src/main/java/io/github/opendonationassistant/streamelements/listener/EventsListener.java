package io.github.opendonationassistant.streamelements.listener;

import io.github.opendonationassistant.events.MessageProcessor;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RabbitListener(executor = "config-listener")
public class EventsListener {

  public static final String QUEUE_NAME = "streamelements.events";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final List<Exchange> BINDING = List.of(
    Exchange.Exchange(
      "history",
      Map.of("event.HistoryItemEvent", EventsListener.QUEUE)
    ),
    Exchange.Exchange(
      "twitch",
      Map.of(
        "event.TwitchChannelFollowEvent",
        EventsListener.QUEUE,
        "event.TwitchChannelSubscriptionMessageEvent",
        EventsListener.QUEUE,
        "event.TwitchChannelRaidEvent",
        EventsListener.QUEUE
      )
    )
  );
  private final MessageProcessor processor;

  @Inject
  public EventsListener(MessageProcessor processor) {
    this.processor = processor;
  }

  @Queue(QUEUE_NAME)
  public void listen(
    @MessageHeader("type") String type,
    byte[] payload,
    RabbitAcknowledgement ack
  ) throws IOException {
    processor.process(type, payload, ack);
  }
}
