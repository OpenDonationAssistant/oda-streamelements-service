package io.github.opendonationassistant.streamelements.repository;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.streamelements.WidgetFacade;
import io.github.opendonationassistant.streamelements.WidgetFacade.Detail;
import io.github.opendonationassistant.streamelements.WidgetFacade.Event;
import io.github.opendonationassistant.streamelements.WidgetFacade.Payload;
import java.util.concurrent.CompletableFuture;

public class StreamElementsSession {

  private final String recipientId;
  private final StreamElementsDataRepository repository;
  private StreamElementsData data;
  private WidgetFacade facade;

  public StreamElementsSession(
    String recipientId,
    StreamElementsData data,
    StreamElementsDataRepository repository,
    WidgetFacade facade
  ) {
    this.recipientId = recipientId;
    this.data = data;
    this.repository = repository;
    this.facade = facade;
  }

  public StreamElementsData data() {
    return data;
  }

  public void setDonationgoalState(Amount collected) {
    this.data = data.withTipGoal(
      new StreamElementsData.Tip("", collected.getMajor().longValue())
    );
    this.save();
  }

  public CompletableFuture<Void> setTipsLatest(
    String name,
    Amount tip,
    String message
  ) {
    this.data = data.withTipLatest(
      new StreamElementsData.Tip(name, tip.getMajor().longValue())
    );
    this.save();
    return facade.sendEvent(
      recipientId,
      new Event(
        new Detail(
          "tip-latest",
          Payload.empty()
            .withName(name)
            .withAmount(tip.getMajor().longValue())
            .withMessage(message)
        )
      )
    );
  }

  public void save() {
    repository.update(recipientId, data);
  }
}
