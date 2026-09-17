package io.github.opendonationassistant.streamelements.overlay;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Defensive accessors over a deserialized StreamElements JSON document. Real
 * payloads contain malformed/absent keys, so every accessor is total: it never
 * throws and always falls back to the supplied default.
 */
final class JsonValues {

  private JsonValues() {}

  @SuppressWarnings("unchecked")
  static Map<String, Object> map(@Nullable Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Map.of();
  }

  @SuppressWarnings("unchecked")
  static List<Object> list(@Nullable Object value) {
    if (value instanceof List<?> list) {
      return (List<Object>) list;
    }
    return List.of();
  }

  static String string(@Nullable Object value, String fallback) {
    if (value == null) {
      return fallback;
    }
    return String.valueOf(value);
  }

  static boolean bool(@Nullable Object value, boolean fallback) {
    if (value instanceof Boolean result) {
      return result;
    }
    if (value instanceof String text) {
      if ("true".equalsIgnoreCase(text)) {
        return true;
      }
      if ("false".equalsIgnoreCase(text)) {
        return false;
      }
    }
    return fallback;
  }
}
