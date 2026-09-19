package io.github.opendonationassistant.streamelements.overlay;

import io.github.opendonationassistant.commons.logging.ODALogger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Parses the overlay id from a StreamElements overlay browser-source URL. */
final class SeOverlayUrl {

  private static ODALogger log = new ODALogger(SeOverlayUrl.class);

  private static final String HOST = "streamelements.com";
  private static final String OVERLAY = "overlay";

  private SeOverlayUrl() {}

  static String parse(@Nullable String url) {
    if (url == null || url.isBlank()) {
      throw new InvalidOverlayUrlException(
        "Overlay URL must not be null or blank"
      );
    }
    var uri = toUri(url.trim());
    if (!isStreamElementsOwned(uri.getHost())) {
      throw new InvalidOverlayUrlException("Overlay URL host is not allowed");
    }
    var segments = pathSegments(uri.getPath());
    if (segments.size() != 3 || !OVERLAY.equals(segments.get(0))) {
      throw new InvalidOverlayUrlException(
        "Overlay URL must match /overlay/{overlayId}/{overlayToken}"
      );
    }
    var overlayId = segments.get(1);
    var overlayToken = segments.get(2);
    if (overlayId.isBlank() || overlayToken.isBlank()) {
      throw new InvalidOverlayUrlException(
        "Overlay URL must match /overlay/{overlayId}/{overlayToken}"
      );
    }
    return overlayId;
  }

  private static URI toUri(String url) {
    try {
      return URI.create(url);
    } catch (IllegalArgumentException exception) {
      throw new InvalidOverlayUrlException(
        "Overlay URL is malformed",
        exception
      );
    }
  }

  /** True when {@code host} is {@code streamelements.com} or one of its subdomains. */
  static boolean isStreamElementsOwned(@Nullable String host) {
    if (host == null) {
      return false;
    }
    var normalized = host.toLowerCase(Locale.ROOT);
    return normalized.equals(HOST) || normalized.endsWith("." + HOST);
  }

  private static List<String> pathSegments(@Nullable String path) {
    if (path == null || path.isBlank()) {
      return List.of();
    }
    if (path.endsWith("/")) {
      throw new InvalidOverlayUrlException(
        "Overlay URL path is malformed - trailing slash"
      );
    }
    var segments = path.split("/");
    return Arrays.stream(segments)
      .dropWhile(String::isBlank)
      .map(it -> {
        var trimmed = it.trim();
        if (trimmed.isBlank()) {
          throw new InvalidOverlayUrlException("Overlay URL path is malformed");
        }
        return trimmed;
      })
      .toList();
  }
}
