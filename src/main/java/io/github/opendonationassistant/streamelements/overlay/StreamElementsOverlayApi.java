package io.github.opendonationassistant.streamelements.overlay;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Declarative HTTP client for the StreamElements REST API. The base URL is a
 * compile-time literal so the AOT cached environment stays valid.
 *
 * <p>Used to resolve the account/channel id from a token
 * ({@code /kappa/v2/channels/me}) and to fetch the raw overlay JSON with all
 * widgets ({@code /kappa/v2/overlays/{channel}/{overlayId}}).
 */
@Client("https://api.streamelements.com")
public interface StreamElementsOverlayApi {
  @Get("/kappa/v2/channels/me")
  Channel me(@Header("Authorization") String authorization);

  @Get("/kappa/v2/overlays/{channel}/{overlayId}")
  String overlay(
    @Header("Authorization") String authorization,
    String channel,
    String overlayId
  );

  /**
   * StreamElements channel/account identity. {@link JsonProperty} maps the
   * {@code id} component to the {@code _id} wire name.
   */
  @Serdeable
  record Channel(@JsonProperty("_id") String id) {}
}
