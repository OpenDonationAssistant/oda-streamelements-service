package io.github.opendonationassistant.streamelements.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.streamelements.repository.StreamElementsSessionRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;

@Controller
public class StreamElementsSessionController extends BaseController {

  private final StreamElementsSessionRepository repository;

  @Inject
  public StreamElementsSessionController(
    StreamElementsSessionRepository repository
  ) {
    this.repository = repository;
  }

  @Get("/streamelements/session")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public CompletableFuture<HttpResponse<StreamElementsSessionView>> getSession(
    Authentication auth
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return CompletableFuture.completedFuture(HttpResponse.notFound());
    }
    var session = repository.getSession(ownerId.get());
    session.data();
    return CompletableFuture.completedFuture(
      HttpResponse.ok(
        new StreamElementsSessionView(
          new StreamElementsSessionView.Channel(ownerId.get()),
          StreamElementsSessionView.Session.of(session)
        )
      )
    );
  }
}
