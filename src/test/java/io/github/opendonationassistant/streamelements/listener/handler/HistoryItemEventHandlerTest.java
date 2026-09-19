package io.github.opendonationassistant.streamelements.listener.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSession;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class HistoryItemEventHandlerTest {

  private final StreamElementsSessionRepository repository = mock(
    StreamElementsSessionRepository.class
  );
  private final StreamElementsSession session = mock(
    StreamElementsSession.class
  );
  private final HistoryItemEventHandler handler = new HistoryItemEventHandler(
    ObjectMapper.getDefault(),
    repository
  );

  @Test
  void handle_shouldSetTipsLatestForPayment() throws Exception {
    givenSession("recipient-1");
    var amount = new Amount(500, 0, "RUB");

    handler.handle(payment("recipient-1", amount, "thanks"));

    verify(session).setTipsLatest("nick", amount, "thanks");
  }

  @Test
  void handle_shouldDefaultMissingMessageToEmpty() throws Exception {
    givenSession("recipient-1");
    var amount = new Amount(500, 0, "RUB");

    handler.handle(payment("recipient-1", amount, null));

    verify(session).setTipsLatest("nick", amount, "");
  }

  @Test
  void handle_shouldIgnoreNonPaymentType() throws Exception {
    givenSession("recipient-1");
    var amount = new Amount(500, 0, "RUB");

    handler.handle(
      newEvent("recipient-1", "origin-1", "subscription", amount, "hi")
    );

    verifyNoInteractions(session);
  }

  @Test
  void handle_shouldIgnoreMissingSession() throws Exception {
    when(repository.getSession(any())).thenReturn(
      CompletableFuture.completedFuture(Optional.empty())
    );

    handler.handle(payment("recipient-1", new Amount(500, 0, "RUB"), "hi"));

    verifyNoInteractions(session);
  }

  private static HistoryItemEvent payment(
    String recipientId,
    Amount amount,
    @Nullable String message
  ) {
    return newEvent(recipientId, "origin-1", "payment", amount, message);
  }

  @SuppressWarnings("NullAway")
  private static HistoryItemEvent newEvent(
    String recipientId,
    @Nullable String originId,
    String type,
    Amount amount,
    @Nullable String message
  ) {
    return new HistoryItemEvent(
      "id-1",
      type,
      recipientId,
      "system",
      originId,
      Instant.parse("2026-09-18T00:00:00Z"),
      "nick",
      amount,
      message,
      List.of(),
      List.of(),
      null
    );
  }

  private void givenSession(String recipientId) {
    when(repository.getSession(recipientId)).thenReturn(
      CompletableFuture.completedFuture(Optional.of(session))
    );
  }
}
