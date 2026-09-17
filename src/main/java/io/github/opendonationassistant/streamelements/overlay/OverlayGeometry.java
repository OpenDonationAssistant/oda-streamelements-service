package io.github.opendonationassistant.streamelements.overlay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Geometry helpers for the conversion: pixel parsing, {@code toSize} mapping to
 * ODA width/height property values, stacking and text-shadow parsing.
 */
final class OverlayGeometry {

  private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d+)?(px)?$");
  private static final Pattern SIZE = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)(?:px)?$");
  private static final Pattern SHADOW = Pattern.compile(
    "^(rgba?\\([^)]*\\)|#[0-9a-fA-F]{3,8}|[a-zA-Z]+)" +
    "\\s+(-?\\d+(?:\\.\\d+)?)px" +
    "\\s+(-?\\d+(?:\\.\\d+)?)px" +
    "\\s+(-?\\d+(?:\\.\\d+)?)px$"
  );

  private OverlayGeometry() {}

  /** {@code number -> v}; numeric strings ({@code "731.00px"}) → {@code parseFloat}; else fallback. */
  static double toNumber(@Nullable Object value, double fallback) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value instanceof String text) {
      var trimmed = text.trim();
      if (NUMERIC.matcher(trimmed).matches()) {
        return parse(
          trimmed.endsWith("px")
            ? trimmed.substring(0, trimmed.length() - 2)
            : trimmed,
          fallback
        );
      }
    }
    return fallback;
  }

  static boolean isNumeric(@Nullable Object value) {
    if (value instanceof Number) {
      return true;
    }
    if (value instanceof String text) {
      return NUMERIC.matcher(text.trim()).matches();
    }
    return false;
  }

  /** Reproduces the spec's {@code toSize(v)} → width/height property value. */
  static Map<String, Object> toSize(@Nullable Object value) {
    if (value instanceof Number number) {
      return size("fixed", number.doubleValue());
    }
    if (value instanceof String text) {
      var trimmed = text.trim();
      if ("100%".equals(trimmed)) {
        return size("max", 100);
      }
      if (trimmed.isEmpty() || "auto".equals(trimmed)) {
        return size("min", 0);
      }
      var matcher = SIZE.matcher(trimmed);
      if (matcher.matches()) {
        return size("fixed", parse(matcher.group(1), 0));
      }
    }
    return size("min", 0);
  }

  static Map<String, Object> size(String type, double value) {
    var result = new LinkedHashMap<String, Object>();
    result.put("type", type);
    result.put("value", normalize(value));
    return result;
  }

  /** Emits integral doubles as ints/longs so pixel values serialize as {@code 1920}, not {@code 1920.0}. */
  static Object normalize(double value) {
    if (
      !Double.isNaN(value) &&
      !Double.isInfinite(value) &&
      value == Math.rint(value)
    ) {
      var asLong = (long) value;
      if (asLong == value && asLong <= Integer.MAX_VALUE && asLong >= Integer.MIN_VALUE) {
        return (int) asLong;
      }
      return asLong;
    }
    return value;
  }

  static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  /** {@code zIndex} is omitted for zero/absent/negative values (relies on DOM order). */
  static @Nullable Integer zIndex(@Nullable Object value) {
    if (value == null) {
      return null;
    }
    var normalized = (int) toNumber(value, 0d);
    return normalized <= 0 ? null : normalized;
  }

  static ShadowParse parseTextShadow(@Nullable Object value) {
    var text = value == null ? "" : String.valueOf(value).trim();
    if (text.isEmpty() || "none".equalsIgnoreCase(text)) {
      return new ShadowParse(List.of(), false);
    }
    var matcher = SHADOW.matcher(text);
    if (matcher.matches()) {
      var shadow = new LinkedHashMap<String, Object>();
      shadow.put("color", matcher.group(1));
      shadow.put("x", normalize(parse(matcher.group(2), 1)));
      shadow.put("y", normalize(parse(matcher.group(3), 1)));
      shadow.put("blur", normalize(parse(matcher.group(4), 1)));
      return new ShadowParse(List.of(shadow), false);
    }
    var fallback = new LinkedHashMap<String, Object>();
    fallback.put("color", "rgb(0, 0, 0)");
    fallback.put("x", 1);
    fallback.put("y", 1);
    fallback.put("blur", 1);
    return new ShadowParse(List.of(fallback), true);
  }

  private static double parse(String text, double fallback) {
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  record ShadowParse(List<Map<String, Object>> shadows, boolean usedFallback) {}
}
