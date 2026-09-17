package io.github.opendonationassistant.streamelements.view;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.streamelements.overlay.OverlayWidgetImportResult;
import io.github.opendonationassistant.streamelements.overlay.OverlayWidgetImporter;
import io.github.opendonationassistant.streamelements.overlay.StreamElementsOverlayClient;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;

@Controller
public class StreamElementsImportOverlayController extends BaseController {

  private final StreamElementsOverlayClient client;
  private final OverlayWidgetImporter importer;

  @Inject
  public StreamElementsImportOverlayController(
    StreamElementsOverlayClient client,
    OverlayWidgetImporter importer
  ) {
    this.client = client;
    this.importer = importer;
  }

  @Post("/streamelements/import-overlay")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public CompletableFuture<HttpResponse<OverlayWidgetImportResult>> importOverlay(
    @Body ImportOverlayRequest request,
    Authentication auth
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return CompletableFuture.completedFuture(
        HttpResponse.<OverlayWidgetImportResult>unauthorized()
      );
    }
    return CompletableFuture.supplyAsync(
      () -> importFor(ownerId.get(), request)
    );
  }

  private HttpResponse<OverlayWidgetImportResult> importFor(
    String recipientId,
    ImportOverlayRequest request
  ) {
    try {
      var overlayJson = client.fetchOverlay(
        request.url(),
        recipientId,
        request.tokenId()
      );
      return HttpResponse.ok(importer.createWidget(overlayJson, recipientId));
    } catch (IllegalArgumentException exception) {
      return HttpResponse.<OverlayWidgetImportResult>badRequest();
    } catch (StreamElementsOverlayClient.SeTokenUnavailableException exception) {
      return HttpResponse.<OverlayWidgetImportResult>unauthorized();
    } catch (RuntimeException exception) {
      return HttpResponse.<OverlayWidgetImportResult>status(
        HttpStatus.BAD_GATEWAY
      );
    }
  }

  @Serdeable
  public record ImportOverlayRequest(String url, String tokenId) {}
}
