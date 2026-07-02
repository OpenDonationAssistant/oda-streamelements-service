package io.github.opendonationassistant.streamelements;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class WidgetFacade {

  private final ODALogger log = new ODALogger(this);
  private final WidgetEventSender sender;
  private final ObjectMapper mapper;

  @Inject
  public WidgetFacade(WidgetEventSender sender, ObjectMapper mapper) {
    this.sender = sender;
    this.mapper = mapper;
  }

  public CompletableFuture<Void> sendEvent(String recipientId, Event event) {
    log.debug(
      "Send message to UI",
      Map.of("recipientId", recipientId, "message", event)
    );

    try {
      return sender.sendEvent(
        "%s.streamelements".formatted(recipientId),
        this.mapper.writeValueAsBytes(event)
      );
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @RabbitClient("amq.topic")
  public interface WidgetEventSender {
    CompletableFuture<Void> sendEvent(@Binding String binding, byte[] message);
  }

  @Serdeable
  public static record Event(Detail detail) {}

  @Serdeable
  public static record Detail(String listener, Payload event) {}

  @Serdeable
  public static record Payload(
    @Nullable String name,
    @Nullable Long amount,
    @Nullable String message,
    @Nullable Boolean gifted,
    @Nullable String sender,
    @Nullable Boolean bulkGifted,
    @Nullable Boolean isCommunityGift,
    @Nullable Boolean playedAsCommunityGift
  ) {
    public static Payload empty() {
      return new Payload(null, null, null, null, null, null, null, null);
    }
    public Payload withName(String name) {
      return new Payload(
        name,
        amount,
        message,
        gifted,
        sender,
        bulkGifted,
        isCommunityGift,
        playedAsCommunityGift
      );
    }
    public Payload withAmount(Long amount) {
      return new Payload(
        name,
        amount,
        message,
        gifted,
        sender,
        bulkGifted,
        isCommunityGift,
        playedAsCommunityGift
      );
    }
    public Payload withMessage(String message) {
      return new Payload(
        name,
        amount,
        message,
        gifted,
        sender,
        bulkGifted,
        isCommunityGift,
        playedAsCommunityGift
      );
    }
    public Payload withGifted(Boolean gifted) {
      return new Payload(
        name,
        amount,
        message,
        gifted,
        sender,
        bulkGifted,
        isCommunityGift,
        playedAsCommunityGift
      );
    }
  }
}
