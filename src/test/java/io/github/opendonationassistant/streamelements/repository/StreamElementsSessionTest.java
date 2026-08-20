package io.github.opendonationassistant.streamelements.repository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InstancioExtension.class)
public class StreamElementsSessionTest {

  StreamElementsDataRepository repository = mock(
    StreamElementsDataRepository.class
  );
  WidgetFacade facade = mock(WidgetFacade.class);

  final String json =
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

  @Test
  public void testUpdatingDonationGoalState(
    @Given String recipientId,
    @Given StreamElementsData data
  ) throws IOException {
    StreamElementsSession session = new StreamElementsSession(
      recipientId,
      data,
      repository,
      facade
    );
    var expectedData = data.withTipGoal(new StreamElementsData.Tip("", 10L));

    WidgetChangedEvent event = ObjectMapper.getDefault()
      .readValue(json, WidgetChangedEvent.class);
    if (event != null) {
      session.apply(event.widget());
      verify(repository).update(recipientId, expectedData);
    }
  }
}
