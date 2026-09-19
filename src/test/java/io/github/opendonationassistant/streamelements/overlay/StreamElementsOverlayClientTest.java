package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenResponse;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;

class StreamElementsOverlayClientTest {

  private static final String OVERLAY_URL =
    "https://streamelements.com/overlay/overlay-1/overlay-token";
  private static final String ASSET_URL =
    "https://cdn.streamelements.com/bar.png";

  private final StreamElementsOverlayApi api = mock(
    StreamElementsOverlayApi.class
  );
  private final TokenRPC tokenRpc = mock(TokenRPC.class);
  private final HttpClient httpClient = mock(HttpClient.class);
  private final BlockingHttpClient blockingClient = mock(
    BlockingHttpClient.class
  );
  private final StreamElementsOverlayClient client =
    new StreamElementsOverlayClient(api, tokenRpc, httpClient);

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
    assertThrows(InvalidOverlayUrlException.class, () ->
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
    when(tokenRpc.token(any())).thenReturn(new TokenResponse(null, "no token"));

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

  @Test
  void downloadAsset_sendsBearerTokenAndReturnsContent() {
    var content = new byte[] { 1, 2, 3 };
    when(httpClient.toBlocking()).thenReturn(blockingClient);
    when(
      blockingClient.retrieve(any(HttpRequest.class), eq(byte[].class))
    ).thenAnswer(invocation -> {
      HttpRequest<?> request = invocation.getArgument(0);
      assertEquals(
        "Bearer se-token",
        request.getHeaders().get(HttpHeaders.AUTHORIZATION)
      );
      return content;
    });

    var downloaded = client.downloadAsset("se-token", ASSET_URL);

    assertArrayEquals(content, downloaded);
  }

  @Test
  void fetchOverlay_throwsWhenChannelIdIsMissing() {
    when(tokenRpc.token(any())).thenReturn(new TokenResponse("se-token", "ok"));
    when(api.me("Bearer se-token")).thenReturn(
      new StreamElementsOverlayApi.Channel(null)
    );

    assertThrows(
      StreamElementsOverlayClient.SeTokenUnavailableException.class,
      () -> client.fetchOverlay(OVERLAY_URL, "user", "token-1")
    );
  }

  @Test
  void fetchOverlay_throwsWhenChannelIsMissing() {
    when(tokenRpc.token(any())).thenReturn(new TokenResponse("se-token", "ok"));
    when(api.me("Bearer se-token")).thenReturn(null);

    assertThrows(
      StreamElementsOverlayClient.SeTokenUnavailableException.class,
      () -> client.fetchOverlay(OVERLAY_URL, "user", "token-1")
    );
  }

  @Test
  void fetchOverlay_mapsUnauthorizedChannelResponseToTokenUnavailable() {
    when(tokenRpc.token(any())).thenReturn(new TokenResponse("se-token", "ok"));
    when(api.me("Bearer se-token")).thenThrow(
      new HttpClientResponseException(
        "unauthorized",
        HttpResponse.unauthorized()
      )
    );

    assertThrows(
      StreamElementsOverlayClient.SeTokenUnavailableException.class,
      () -> client.fetchOverlay(OVERLAY_URL, "user", "token-1")
    );
  }
}
