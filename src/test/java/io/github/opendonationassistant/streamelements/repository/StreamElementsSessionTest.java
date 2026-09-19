package io.github.opendonationassistant.streamelements.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import io.github.opendonationassistant.streamelements.WidgetFacade.Detail;
import io.github.opendonationassistant.streamelements.WidgetFacade.Event;
import io.github.opendonationassistant.streamelements.WidgetFacade.Payload;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
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
                        "mode": "default",
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

  private static final StreamElementsData EMPTY = new StreamElementsData(
    new StreamElementsData.Tip("", 0L),
    new StreamElementsData.Tip("", 0L),
    null,
    null,
    null
  );

  @Test
  void setFollowLatest_shouldPersistAndPublishFollowerEvent() {
    allowEvents();
    var session = newSession();

    session.setFollowLatest("follower");

    verify(repository).update(
      "recipient-1",
      EMPTY.withFollowerLatest(new StreamElementsData.Follower("follower"))
    );
    verify(facade).sendEvent(
      "recipient-1",
      new Event(
        new Detail("follower-latest", Payload.empty().withName("follower"))
      )
    );
  }

  @Test
  void setSubscriberLatest_shouldPersistAndPublishSubscriberEvent() {
    allowEvents();
    var session = newSession();
    var subscriber = new StreamElementsData.Subscriber(
      "sub",
      "1000",
      "msg",
      3,
      6,
      2
    );

    session.setSubscriberLatest(subscriber);

    verify(repository).update(
      "recipient-1",
      EMPTY.withSubscriberLatest(subscriber)
    );
    verify(facade).sendEvent(
      "recipient-1",
      new Event(
        new Detail(
          "subscriber-latest",
          Payload.empty().withName("sub").withMessage("msg")
        )
      )
    );
  }

  @Test
  void setRaidLatest_shouldPersistAndPublishRaidEvent() {
    allowEvents();
    var session = newSession();
    var raid = new StreamElementsData.Raid("raider", 42);

    session.setRaidLatest(raid);

    verify(repository).update("recipient-1", EMPTY.withRaidLatest(raid));
    verify(facade).sendEvent(
      "recipient-1",
      new Event(
        new Detail(
          "raid-latest",
          Payload.empty().withName("raider").withAmount(42L)
        )
      )
    );
  }

  @Test
  void setTipsLatest_shouldPersistAndPublishTipEvent() {
    allowEvents();
    var session = newSession();

    session.setTipsLatest("nick", new Amount(500, 0, "RUB"), "msg");

    verify(repository).update(
      "recipient-1",
      EMPTY.withTipLatest(new StreamElementsData.Tip("nick", 500L))
    );
    verify(facade).sendEvent(
      "recipient-1",
      new Event(
        new Detail(
          "tip-latest",
          Payload.empty().withName("nick").withAmount(500L).withMessage("msg")
        )
      )
    );
  }

  @Test
  void setDonationgoalState_shouldPersistWithoutPublishingEvent() {
    var session = newSession();

    session.setDonationgoalState(new Amount(10, 0, "RUB"));

    verify(repository).update(
      "recipient-1",
      EMPTY.withTipGoal(new StreamElementsData.Tip("", 10L))
    );
    verifyNoInteractions(facade);
  }

  private StreamElementsSession newSession() {
    return new StreamElementsSession("recipient-1", EMPTY, repository, facade);
  }

  private void allowEvents() {
    when(facade.sendEvent(any(), any())).thenReturn(
      CompletableFuture.completedFuture(null)
    );
  }
}
