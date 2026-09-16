package io.github.opendonationassistant.streamelements.repository;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.streamelements.WidgetConfigClient;
import io.github.opendonationassistant.streamelements.WidgetConfigClient.WidgetConfigRequest;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StreamElementsSessionRepository {

  private static final String DONATION_GOAL_WIDGET_TYPE = "donationgoal";

  private final ODALogger log = new ODALogger(this);
  private final StreamElementsDataRepository sessions;
  private final WidgetFacade facade;
  private final WidgetConfigClient configClient;

  @Inject
  public StreamElementsSessionRepository(
    StreamElementsDataRepository sessions,
    WidgetFacade facade,
    WidgetConfigClient configClient
  ) {
    this.sessions = sessions;
    this.facade = facade;
    this.configClient = configClient;
  }

  public CompletableFuture<Optional<StreamElementsSession>> getSession(
    String recipientId
  ) {
    return CompletableFuture.completedFuture(
      sessions.get(recipientId).map(data -> convert(recipientId, data))
    );
  }

  private StreamElementsSession convert(
    String recipientId,
    StreamElementsData data
  ) {
    return new StreamElementsSession(recipientId, data, sessions, facade);
  }

  public CompletableFuture<StreamElementsSession> createSession(
    String recipientId
  ) {
    var data = new StreamElementsData(
      new StreamElementsData.Tip("", 0L),
      new StreamElementsData.Tip("", 0L),
      null,
      null,
      null
    );
    sessions.update(recipientId, data);
    var session = convert(recipientId, data);
    return applyWidgetConfig(recipientId, session).thenApply(it -> session);
  }

  private CompletableFuture<Void> applyWidgetConfig(
    String recipientId,
    StreamElementsSession session
  ) {
    return CompletableFuture.supplyAsync(() ->
      configClient.request(
        new WidgetConfigRequest(null, DONATION_GOAL_WIDGET_TYPE)
      )
    )
      .thenAccept(widgets -> {
        widgets
          .stream()
          .filter(widget -> recipientId.equals(widget.ownerId()))
          .forEach(session::apply);
      })
      .exceptionally(error -> {
        log.error(
          "Failed to load widget configs",
          Map.of(
            "recipientId",
            recipientId,
            "error",
            String.valueOf(error.getMessage())
          )
        );
        return null;
      });
  }
}
