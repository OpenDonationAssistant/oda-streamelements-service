package io.github.opendonationassistant.streamelements.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.events.widget.Widget.WidgetConfig;
import io.github.opendonationassistant.events.widget.Widget.WidgetProperty;
import io.github.opendonationassistant.streamelements.WidgetConfigClient;
import io.github.opendonationassistant.streamelements.WidgetConfigClient.WidgetConfigRequest;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InstancioExtension.class)
class StreamElementsSessionRepositoryTest {

  private final WidgetConfigClient client = mock(WidgetConfigClient.class);
  private final WidgetFacade facade = mock(WidgetFacade.class);
  private final StreamElementsDataRepository data =
    new StreamElementsDataRepository(new HashMap<>());
  private final StreamElementsSessionRepository repository =
    new StreamElementsSessionRepository(data, facade, client);

  private Widget donationGoalWidget(int accumulatedMajor, String ownerId) {
    return new Widget(
      "widget-id",
      "donationgoal",
      0,
      "Donation goal",
      true,
      ownerId,
      false,
      new WidgetConfig(
        List.of(
          new WidgetProperty(
            "goal",
            "Goal",
            "goal",
            List.of(
              Map.of(
                "id",
                "goal-id",
                "mode",
                "default",
                "requiredAmount",
                Map.of("major", 100, "currency", "RUB"),
                "accumulatedAmount",
                Map.of("major", accumulatedMajor, "currency", "RUB")
              )
            )
          )
        )
      )
    );
  }

  @Test
  void startSession_shouldRequestWidgetConfigsAndFillDonationGoal(
    @Given String recipientId,
    @Given int amount
  ) {
    when(client.request(any())).thenReturn(
      List.of(donationGoalWidget(amount, recipientId))
    );

    var session = repository.startSession(recipientId).join();

    verify(client).request(new WidgetConfigRequest(null, "donationgoal"));
    assertEquals(amount, session.data().tipGoal().amount());
  }

  @Test
  void startSession_shouldIgnoreWidgetsOfOtherOwners(
    @Given String recipientId,
    @Given Integer amount
  ) {
    when(client.request(any())).thenReturn(
      List.of(donationGoalWidget(amount, "anotheruser"))
    );

    var session = repository.startSession(recipientId).join();

    assertEquals(0L, session.data().tipGoal().amount());
  }

  @Test
  void startSession_shouldIgnoreNonDonationGoalWidgets(
    @Given String recipientId
  ) {
    when(client.request(any())).thenReturn(
      List.of(
        new Widget(
          "widget-id",
          "payment-alerts",
          0,
          "Alerts",
          true,
          recipientId,
          false,
          new WidgetConfig(List.of())
        )
      )
    );

    var session = repository.startSession(recipientId);

    assertEquals(0L, session.join().data().tipGoal().amount());
  }

  @Test
  void startSession_shouldHandleMissingWidgetConfig(@Given String recipientId) {
    when(client.request(any())).thenReturn(List.of());

    var session = repository.startSession(recipientId);

    assertEquals(0L, session.join().data().tipGoal().amount());
  }
}
