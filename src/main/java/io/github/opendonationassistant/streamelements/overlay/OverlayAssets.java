package io.github.opendonationassistant.streamelements.overlay;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.micronaut.serde.annotation.Serdeable;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Re-hosts StreamElements-owned overlay assets (images, videos, audio, and the
 * canvas background) on the ODA CDN.
 *
 * <p>Only assets served from StreamElements hosts are downloaded and uploaded,
 * so the bearer token is never sent to third-party hosts. A failed asset keeps
 * its original URL; the import itself never fails because of one asset.
 */
final class OverlayAssets {

  private static final String CDN_PREFIX = "https://cdn.oda.digital/files/";

  private OverlayAssets() {}

  static OverlayConversion rehost(
    OverlayConversion conversion,
    String recipientId,
    String token,
    StreamElementsOverlayClient client,
    RabbitClient commandsFacade,
    ODALogger log
  ) {
    var elements = conversion
      .elements()
      .stream()
      .map(element ->
        rehostElement(element, recipientId, token, client, commandsFacade, log)
      )
      .toList();
    return new OverlayConversion(
      conversion.name(),
      elements,
      conversion.warnings()
    );
  }

  private static OverlayElement rehostElement(
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

  private static void rehostUrlSetting(
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

  private static String rehost(
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
      return CDN_PREFIX + filename;
    } catch (RuntimeException exception) {
      log.warn(
        "Could not rehost StreamElements asset",
        Map.of("url", url, "error", exception.getClass().getSimpleName())
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
