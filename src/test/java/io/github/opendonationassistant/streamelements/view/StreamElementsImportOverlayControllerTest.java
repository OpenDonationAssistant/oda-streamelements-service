package io.github.opendonationassistant.streamelements.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.streamelements.overlay.ConversionWarning;
import io.github.opendonationassistant.streamelements.overlay.OverlayWidgetImportResult;
import io.github.opendonationassistant.streamelements.overlay.OverlayWidgetImporter;
import io.github.opendonationassistant.streamelements.overlay.StreamElementsOverlayClient;
import io.micronaut.http.HttpStatus;
import io.micronaut.security.authentication.Authentication;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class StreamElementsImportOverlayControllerTest {

  private static final String OVERLAY_URL =
    "https://streamelements.com/overlay/overlay-1/token";

  private final StreamElementsOverlayClient client = mock(
    StreamElementsOverlayClient.class
  );
  private final OverlayWidgetImporter importer = mock(
    OverlayWidgetImporter.class
  );
  private final StreamElementsImportOverlayController controller =
    new StreamElementsImportOverlayController(client, importer);
  private final StreamElementsImportOverlayController.ImportOverlayRequest request =
    new StreamElementsImportOverlayController.ImportOverlayRequest(
      OVERLAY_URL,
      "token-1"
    );

  @Test
  void importOverlay_returnsCreatedWidgetResult() {
    when(client.fetchOverlay(OVERLAY_URL, "user", "token-1")).thenReturn("{}");
    var warning = new ConversionWarning(
      "25",
      "chat",
      "CHAT",
      "reason",
      "placeholder"
    );
    when(importer.createWidget("{}", "user")).thenReturn(
      new OverlayWidgetImportResult("widget-1", List.of(warning))
    );

    var response = controller.importOverlay(request, auth("user")).join();

    assertEquals(HttpStatus.OK, response.getStatus());
    assertEquals("widget-1", response.getBody().orElseThrow().widgetId());
  }

  @Test
  void importOverlay_returnsUnauthorizedWhenOwnerMissing() {
    var response = controller.importOverlay(request, auth(null)).join();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    verifyNoInteractions(client, importer);
  }

  @Test
  void importOverlay_returnsBadRequestForInvalidUrl() {
    when(client.fetchOverlay(any(), any(), any())).thenThrow(
      new IllegalArgumentException("bad url")
    );

    var response = controller.importOverlay(request, auth("user")).join();

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    verifyNoInteractions(importer);
  }

  @Test
  void importOverlay_returnsUnauthorizedWhenTokenUnavailable() {
    when(client.fetchOverlay(any(), any(), any())).thenThrow(
      new StreamElementsOverlayClient.SeTokenUnavailableException("no token")
    );

    var response = controller.importOverlay(request, auth("user")).join();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    verifyNoInteractions(importer);
  }

  @Test
  void importOverlay_returnsBadGatewayForUpstreamFailure() {
    when(client.fetchOverlay(any(), any(), any())).thenThrow(
      new IllegalStateException("upstream down")
    );

    var response = controller.importOverlay(request, auth("user")).join();

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatus());
    verifyNoInteractions(importer);
  }

  private static Authentication auth(@Nullable String username) {
    var authentication = mock(Authentication.class);
    when(authentication.getAttributes()).thenReturn(
      username == null
        ? Map.of()
        : Map.<String, Object>of("preferred_username", username)
    );
    return authentication;
  }
}
