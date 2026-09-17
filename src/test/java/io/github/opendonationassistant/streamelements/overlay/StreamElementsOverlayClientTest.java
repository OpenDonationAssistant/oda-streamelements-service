package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenResponse;
import org.junit.jupiter.api.Test;

class StreamElementsOverlayClientTest {

  private static final String OVERLAY_URL =
    "https://streamelements.com/overlay/overlay-1/overlay-token";

  private final StreamElementsOverlayApi api = mock(
    StreamElementsOverlayApi.class
  );
  private final TokenRPC tokenRpc = mock(TokenRPC.class);
  private final StreamElementsOverlayClient client =
    new StreamElementsOverlayClient(api, tokenRpc);

  @Test
  void fetchOverlay_resolvesChannelAndReturnsOverlayJson() {
    when(tokenRpc.token(new TokenRequest("user", "token-1"))).thenReturn(
      new TokenResponse("se-token", "ok")
    );
    when(api.me("Bearer se-token")).thenReturn(
      new StreamElementsOverlayApi.Channel("channel-1")
    );
    when(api.overlay("Bearer se-token", "channel-1", "overlay-1")).thenReturn(
      "{\"name\":\"scene\"}"
    );

    var json = client.fetchOverlay(OVERLAY_URL, "user", "token-1");

    assertEquals("{\"name\":\"scene\"}", json);
    verify(api).me("Bearer se-token");
    verify(api).overlay("Bearer se-token", "channel-1", "overlay-1");
  }

  @Test
  void fetchOverlay_rejectsInvalidUrlBeforeResolvingToken() {
    assertThrows(
      IllegalArgumentException.class,
      () ->
        client.fetchOverlay(
          "https://evil.com/overlay/overlay-1/token",
          "user",
          "token-1"
        )
    );
    verifyNoInteractions(tokenRpc, api);
  }

  @Test
  void fetchOverlay_throwsWhenTokenIsNull() {
    when(tokenRpc.token(any())).thenReturn(
      new TokenResponse(null, "no token")
    );

    assertThrows(
      StreamElementsOverlayClient.SeTokenUnavailableException.class,
      () -> client.fetchOverlay(OVERLAY_URL, "user", "token-1")
    );
    verifyNoInteractions(api);
  }

  @Test
  void fetchOverlay_throwsWhenTokenIsBlank() {
    when(tokenRpc.token(any())).thenReturn(
      new TokenResponse("   ", "blank token")
    );

    assertThrows(
      StreamElementsOverlayClient.SeTokenUnavailableException.class,
      () -> client.fetchOverlay(OVERLAY_URL, "user", "token-1")
    );
    verifyNoInteractions(api);
  }
}
