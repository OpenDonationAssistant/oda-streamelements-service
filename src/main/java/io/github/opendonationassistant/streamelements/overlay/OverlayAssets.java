package io.github.opendonationassistant.streamelements.overlay;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.micronaut.context.annotation.Value;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Re-hosts StreamElements-owned overlay assets (images, videos, audio, and the
 * canvas background) on the ODA CDN.
 *
 * <p>Only assets served from StreamElements hosts are downloaded and uploaded,
 * so the bearer token is never sent to third-party hosts. A failed asset keeps
 * its original URL; the import itself never fails because of one asset.
 */
@Singleton
public class OverlayAssets {

  private final String cdnBaseUrl;

  @Inject
  public OverlayAssets(@Value("${oda.cdn.base-url}") String cdnBaseUrl) {
    this.cdnBaseUrl = cdnBaseUrl;
  }

  OverlayConversion rehost(
    OverlayConversion conversion,
    String recipientId,
    String token,
    StreamElementsOverlayClient client,
    RabbitClient commandsFacade,
    ODALogger log
  ) {
    var elements = rehostAll(
      conversion.elements(),
      recipientId,
      token,
      client,
      commandsFacade,
      log
    );
    return new OverlayConversion(
      conversion.name(),
      elements,
      conversion.warnings()
    );
  }

  /**
   * Re-hosts every element concurrently on virtual threads while preserving the
   * original element order. Asset-level failures stay isolated in
   * {@link #rehost(String, String, String, StreamElementsOverlayClient,
   * RabbitClient, ODALogger)}, so one bad asset never fails the whole import.
   */
  private List<OverlayElement> rehostAll(
    List<OverlayElement> source,
    String recipientId,
    String token,
    StreamElementsOverlayClient client,
    RabbitClient commandsFacade,
    ODALogger log
  ) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = source
        .stream()
        .map(element ->
          CompletableFuture.supplyAsync(
            () ->
              rehostElement(
                element,
                recipientId,
                token,
                client,
                commandsFacade,
                log
              ),
            executor
          )
        )
        .toList();
      return futures.stream().map(CompletableFuture::join).toList();
    }
  }

  private OverlayElement rehostElement(
    OverlayElement element,
    String recipientId,
    String token,
    StreamElementsOverlayClient client,
    RabbitClient commandsFacade,
    ODALogger log
  ) {
    var settings = new LinkedHashMap<String, Object>(element.settings());
    rehostUrlSetting(settings, recipientId, token, client, commandsFacade, log);
    if (settings.get("backgroundImage") instanceof Map<?, ?> background) {
      var backgroundSettings = new LinkedHashMap<String, Object>(
        JsonValues.map(background)
      );
      rehostUrlSetting(
        backgroundSettings,
        recipientId,
        token,
        client,
        commandsFacade,
        log
      );
      settings.put("backgroundImage", backgroundSettings);
    }
    return new OverlayElement(
      element.id(),
      element.name(),
      element.type(),
      element.enabled(),
      element.containerId(),
      element.level(),
      element.advanced(),
      element.advancedLevel(),
      settings,
      element.order()
    );
  }

  private void rehostUrlSetting(
    Map<String, Object> settings,
    String recipientId,
    String token,
    StreamElementsOverlayClient client,
    RabbitClient commandsFacade,
    ODALogger log
  ) {
    if (settings.get("url") instanceof String url && !url.isBlank()) {
      settings.put(
        "url",
        rehost(url, recipientId, token, client, commandsFacade, log)
      );
    }
  }

  private String rehost(
    String url,
    String recipientId,
    String token,
    StreamElementsOverlayClient client,
    RabbitClient commandsFacade,
    ODALogger log
  ) {
    if (!SeOverlayUrl.isStreamElementsOwned(host(url))) {
      return url;
    }
    try {
      var filename = filename(url);
      var content = client.downloadAsset(token, url);
      commandsFacade.sendCommand(
        new UploadFileCommand(recipientId, content, filename)
      );
      return cdnBaseUrl + filename;
    } catch (RuntimeException exception) {
      log.warn(
        "Could not rehost StreamElements asset",
        Map.of(
          "url", url,
          "error", exception.getClass().getSimpleName(),
          "message", String.valueOf(exception.getMessage())
        )
      );
      return url;
    }
  }

  private static String host(String url) {
    try {
      var host = URI.create(url).getHost();
      return host == null ? "" : host;
    } catch (IllegalArgumentException exception) {
      return "";
    }
  }

  private static String filename(String url) {
    return (
      Generators.timeBasedEpochGenerator().generate() + "." + extension(url)
    );
  }

  private static String extension(String url) {
    var path = path(url);
    var dot = path.lastIndexOf('.');
    if (dot < 0 || dot == path.length() - 1) {
      return "bin";
    }
    var extension = path.substring(dot + 1);
    return extension.matches("[A-Za-z0-9]{1,8}") ? extension : "bin";
  }

  private static String path(String url) {
    try {
      var path = URI.create(url).getPath();
      return path == null ? "" : path;
    } catch (IllegalArgumentException exception) {
      return "";
    }
  }

  @Serdeable
  public record UploadFileCommand(
    String recipientId,
    byte[] content,
    String filename
  ) {}
}
