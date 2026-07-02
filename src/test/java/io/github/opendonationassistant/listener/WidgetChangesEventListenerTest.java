package io.github.opendonationassistant.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.streamelements.listener.WidgetChangesEventListener;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSession;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.serde.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WidgetChangesEventListenerTest {

  String json =
    """
          {
            "type": "updated",
            "widget": {
              "id": "019f1ab3-dc27-7f52-b8f9-4705b5d79a9b",
              "type": "donationgoal",
              "sortOrder": 0,
              "name": "Donation goal",
              "enabled": true,
              "ownerId": "testuser",
              "config": {
                "properties": [
                  {
                    "name": "goal",
                    "value": [
                      {
                        "id": "019f1ab3-dc2c-7835-b5ca-19824bbee2ad",
                        "briefDescription": "name",
                        "fullDescription": "",
                        "default": true,
                        "requiredAmount": { "major": 100, "currency": "RUB" },
                        "accumulatedAmount": { "major": 10, "currency": "RUB" }
                      }
                    ]
                  }
                ]
              }
            },
            "source": "manual"
          }
    """;

  @Mock
  StreamElementsSessionRepository repository = mock(StreamElementsSessionRepository.class);

  @Test
  @SuppressWarnings("unchecked")
  void handleDonationGoalEvent_shouldSetDonationgoalStateOnSession()
    throws Exception {
    StreamElementsSession session = mock(StreamElementsSession.class);
    when(repository.getSession(any())).thenReturn(session);

    WidgetChangedEvent event = ObjectMapper.getDefault()
      .readValue(json, WidgetChangedEvent.class);

    if (event != null) {
      new WidgetChangesEventListener(repository).handle(event);
    }

    verify(session).setDonationgoalState(new Amount(10, 0, "RUB"));
  }
}
