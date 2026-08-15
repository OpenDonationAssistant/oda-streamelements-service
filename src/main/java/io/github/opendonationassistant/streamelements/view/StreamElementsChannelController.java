package io.github.opendonationassistant.streamelements.view;

import io.github.opendonationassistant.streamelements.repository.StreamElementsDataRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;

@Controller
public class StreamElementsChannelController {

  private final StreamElementsDataRepository repository;

  @Inject
  public StreamElementsChannelController(
    StreamElementsDataRepository repository
  ) {
    this.repository = repository;
  }

  @Get("/streamelements/channels/{channel}")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public CompletableFuture<HttpResponse<StreamElementsChannelView>> getChannel(
    @PathVariable String channel
  ) {
    // if (repository.get(channel).isEmpty()) {
    //   return CompletableFuture.completedFuture(HttpResponse.unauthorized());
    // }
    return CompletableFuture.completedFuture(
      HttpResponse.ok(StreamElementsChannelView.of(channel))
    );
  }
}
