package io.github.opendonationassistant.streamelements.overlay;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.rabbit.TokenRPC;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

/**
 * Orchestrates a StreamElements overlay fetch: resolves an ODA-stored
 * StreamElements token via {@link TokenRPC}, resolves the account/channel id
 * with {@code GET /channels/me}, then retrieves the raw overlay JSON. The token
 * is secret material and is never logged or placed in the {@link ODALogger}
 * context.
 */
@Singleton
public class StreamElementsOverlayClient {

  private final ODALogger log = new ODALogger(this);
  private final StreamElementsOverlayApi api;
  private final TokenRPC tokenRpc;

  @Inject
  public StreamElementsOverlayClient(
    StreamElementsOverlayApi api,
    TokenRPC tokenRpc
  ) {
    this.api = api;
    this.tokenRpc = tokenRpc;
  }

  public String fetchOverlay(
    String overlayUrl,
    String recipientId,
    String tokenId
  ) {
    var overlayId = SeOverlayUrl.parse(overlayUrl);
    var response = tokenRpc.token(
      new TokenRPC.TokenRequest(recipientId, tokenId)
    );
    var token = response.token();
    if (token == null || token.isBlank()) {
      throw new SeTokenUnavailableException(
        "StreamElements token is unavailable: " + response.message()
      );
    }
    var authorization = "Bearer " + token;
    var channel = api.me(authorization);
    log.info(
      "Fetching StreamElements overlay",
      Map.of("recipientId", recipientId, "overlayId", overlayId)
    );
    return api.overlay(authorization, channel.id(), overlayId);
  }

  /** Raised when TokenRPC returns no usable StreamElements token. */
  public static class SeTokenUnavailableException extends RuntimeException {
    public SeTokenUnavailableException(String message) {
      super(message);
    }
  }
}
