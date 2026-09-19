package io.github.opendonationassistant.streamelements.overlay;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.streamelements.overlay.StreamElementsOverlayApi.Channel;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Orchestrates StreamElements access: resolves an ODA-stored StreamElements
 * token via {@link TokenRPC}, resolves the account/channel id with
 * {@code GET /channels/me}, retrieves the raw overlay JSON, and downloads
 * protected assets. The token is secret material and is never logged or placed
 * in the {@link ODALogger} context.
 */
@Singleton
public class StreamElementsOverlayClient {

  private final ODALogger log = new ODALogger(this);
  private final StreamElementsOverlayApi api;
  private final TokenRPC tokenRpc;
  private final HttpClient httpClient;

  @Inject
  public StreamElementsOverlayClient(
    StreamElementsOverlayApi api,
    TokenRPC tokenRpc,
    HttpClient httpClient
  ) {
    this.api = api;
    this.tokenRpc = tokenRpc;
    this.httpClient = httpClient;
  }

  public String fetchOverlay(
    String overlayUrl,
    String recipientId,
    String tokenId
  ) {
    var overlayId = SeOverlayUrl.parse(overlayUrl);
    var token = resolveToken(recipientId, tokenId);
    var authorization = "Bearer " + token;
    var channelId = resolveChannelId(authorization);
    log.info(
      "Fetching StreamElements overlay",
      Map.of("recipientId", recipientId, "overlayId", overlayId)
    );
    return api.overlay(authorization, channelId, overlayId);
  }

  /** Resolves the StreamElements bearer token, failing when unavailable. */
  public String resolveToken(String recipientId, String tokenId) {
    var response = tokenRpc.token(
      new TokenRPC.TokenRequest(recipientId, tokenId)
    );
    if (response == null) {
      throw new SeTokenUnavailableException(
        "StreamElements token RPC returned no response"
      );
    }
    var token = response.token();
    if (token == null || token.isBlank()) {
      throw new SeTokenUnavailableException(
        "StreamElements token is unavailable: " + response.message()
      );
    }
    return token;
  }

  /**
   * Downloads a StreamElements-hosted asset with the resolved token. Only call
   * this for StreamElements-owned hosts; the token must never be sent to
   * third-party hosts.
   */
  public byte[] downloadAsset(String token, String url) {
    var request = HttpRequest.GET(url)
      .accept(MediaType.APPLICATION_OCTET_STREAM)
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    return httpClient.toBlocking().retrieve(request, byte[].class);
  }

  private String resolveChannelId(String authorization) {
    @Nullable Channel channel = fetchChannel(authorization);
    if (channel == null) {
      throw new SeTokenUnavailableException(
        "StreamElements account could not be resolved"
      );
    }
    var channelId = channel.id();
    if (channelId == null || channelId.isBlank()) {
      throw new SeTokenUnavailableException(
        "StreamElements account could not be resolved"
      );
    }
    return channelId;
  }

  @Nullable
  private Channel fetchChannel(String authorization) {
    try {
      return api.me(authorization);
    } catch (HttpClientResponseException exception) {
      if (isAuthRejection(exception)) {
        throw new SeTokenUnavailableException(
          "StreamElements rejected the token"
        );
      }
      throw exception;
    }
  }

  private static boolean isAuthRejection(HttpClientResponseException exception) {
    var status = exception.getStatus();
    return status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN;
  }

  /** Raised when TokenRPC returns no usable StreamElements token. */
  public static class SeTokenUnavailableException extends RuntimeException {
    public SeTokenUnavailableException(String message) {
      super(message);
    }
  }
}
