package io.github.opendonationassistant.streamelements.overlay;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/** Parses the overlay id from a StreamElements overlay browser-source URL. */
final class SeOverlayUrl {

  private static final String HOST = "streamelements.com";
  private static final String OVERLAY = "overlay";

  private SeOverlayUrl() {}

  static String parse(@Nullable String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Overlay URL must not be null or blank");
    }
    var uri = URI.create(url.trim());
    if (!isAllowedHost(uri.getHost())) {
      throw new IllegalArgumentException("Overlay URL host is not allowed");
    }
    var segments = pathSegments(uri.getPath());
    if (segments.size() != 3 || !OVERLAY.equals(segments.get(0))) {
      throw new IllegalArgumentException(
        "Overlay URL must match /overlay/{overlayId}/{overlayToken}"
      );
    }
    return segments.get(1);
  }

  private static boolean isAllowedHost(@Nullable String host) {
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
    return Arrays.stream(path.split("/")).filter(part -> !part.isBlank()).toList();
  }
}
