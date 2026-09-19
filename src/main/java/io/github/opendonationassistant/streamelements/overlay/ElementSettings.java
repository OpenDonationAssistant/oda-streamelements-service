package io.github.opendonationassistant.streamelements.overlay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

final class ElementSettings {

  static final String LABEL = "label";
  static final String MEDIA = "media";
  static final String FIXED_COORDINATES_CONTAINER =
    "fixed-coordinates-container";
  static final String ANIMATIONS = "animations";

  private static final String DEFAULT_COLOR = "#FFFFFF";
  private static final String DEFAULT_BORDER_COLOR = "#000000";
  private static final String DEFAULT_FONT_FAMILY = "Roboto";
  private static final double DEFAULT_FONT_SIZE = 24d;
  private static final String DEFAULT_FONT_COLOR = "#684aff";

  private ElementSettings() {}

  // ---------------------------------------------------------------- element defaults

  static Map<String, Object> label() {
    var value = new LinkedHashMap<String, Object>();
    value.put("value", "");
    value.put(
      "font",
      font(
        DEFAULT_FONT_FAMILY,
        DEFAULT_FONT_SIZE,
        DEFAULT_FONT_COLOR,
        false,
        false,
        false,
        List.of()
      )
    );
    value.put("align", "left");
    value.put("justify", "center");
    value.put("direction", "row");
    value.put("backgroundImage", image());
    value.put("backgroundColor", color(DEFAULT_COLOR));
    value.put("width", OverlayGeometry.size("max", 100));
    value.put("height", OverlayGeometry.size("min", 100));
    value.put("border", border());
    value.put("padding", padding());
    value.put("rounding", rounding());
    value.put("shadow", shadow());
    value.put("animation", animation("none", 0));
    return value;
  }

  static Map<String, Object> media() {
    var value = new LinkedHashMap<String, Object>();
    value.put("name", null);
    value.put("url", null);
    value.put("type", "image");
    value.put("opacity", 1);
    value.put("border", border());
    value.put("padding", padding());
    value.put("rounding", rounding());
    value.put("width", OverlayGeometry.size("min", 100));
    value.put("height", OverlayGeometry.size("max", 100));
    value.put("backgroundColor", color(DEFAULT_COLOR));
    value.put("shadow", shadow());
    value.put("animation", animation("none", 0));
    return value;
  }

  static Map<String, Object> fixedCoordinatesContainer(
    double width,
    double height
  ) {
    var value = new LinkedHashMap<String, Object>();
    value.put("positions", new LinkedHashMap<String, Object>());
    value.put("rotation", 0);
    value.put("backgroundImage", image());
    value.put("backgroundColor", color(DEFAULT_COLOR));
    value.put("width", OverlayGeometry.size("fixed", width));
    value.put("height", OverlayGeometry.size("fixed", height));
    value.put("border", border());
    value.put("padding", padding());
    value.put("rounding", rounding());
    value.put("shadow", shadow());
    value.put("animation", animation("none", 0));
    return value;
  }

  static Map<String, Object> animations(String in, String out) {
    var value = new LinkedHashMap<String, Object>();
    value.put("inAnimation", animation(in, 1000));
    value.put("outAnimation", animation(out, 1000));
    return value;
  }

  // ---------------------------------------------------------------- shared values

  static Map<String, Object> color(String hex) {
    var value = new LinkedHashMap<String, Object>();
    value.put("gradient", false);
    value.put("gradientType", 0);
    value.put("repeating", false);
    var stop = new LinkedHashMap<String, Object>();
    stop.put("color", hex);
    value.put("colors", List.of(stop));
    value.put("angle", 0);
    return value;
  }

  static Map<String, Object> border() {
    var value = new LinkedHashMap<String, Object>();
    value.put("isSame", null);
    value.put("bottom", borderSide());
    value.put("top", borderSide());
    value.put("left", borderSide());
    value.put("right", borderSide());
    return value;
  }

  private static Map<String, Object> borderSide() {
    var side = new LinkedHashMap<String, Object>();
    side.put("width", 1);
    side.put("type", "solid");
    side.put("color", DEFAULT_BORDER_COLOR);
    return side;
  }

  static Map<String, Object> padding() {
    var value = new LinkedHashMap<String, Object>();
    value.put("isSame", null);
    value.put("bottom", 0);
    value.put("top", 0);
    value.put("left", 0);
    value.put("right", 0);
    return value;
  }

  static Map<String, Object> rounding() {
    var value = new LinkedHashMap<String, Object>();
    value.put("isSame", null);
    value.put("bottomLeft", 0);
    value.put("bottomRight", 0);
    value.put("topLeft", 0);
    value.put("topRight", 0);
    return value;
  }

  static Map<String, Object> shadow() {
    var value = new LinkedHashMap<String, Object>();
    value.put("shadows", List.of());
    return value;
  }

  static Map<String, Object> image() {
    return image(null, null);
  }

  static Map<String, Object> image(
    @Nullable String name,
    @Nullable String url
  ) {
    var value = new LinkedHashMap<String, Object>();
    value.put("name", name);
    value.put("url", url);
    value.put("size", "auto");
    value.put("repeat", false);
    value.put("opacity", 1);
    return value;
  }

  static Map<String, Object> animation(String animation, double duration) {
    var value = new LinkedHashMap<String, Object>();
    value.put("animation", animation);
    value.put("duration", OverlayGeometry.normalize(duration));
    return value;
  }

  static Map<String, Object> font(
    String family,
    double size,
    String colorHex,
    boolean weight,
    boolean italic,
    boolean underline,
    List<Map<String, Object>> shadows
  ) {
    var value = new LinkedHashMap<String, Object>();
    value.put("family", family);
    value.put("size", OverlayGeometry.normalize(size));
    value.put("color", color(colorHex));
    var outline = new LinkedHashMap<String, Object>();
    outline.put("enabled", false);
    outline.put("width", 0);
    outline.put("color", DEFAULT_BORDER_COLOR);
    value.put("outline", outline);
    value.put("weight", weight);
    value.put("italic", italic);
    value.put("underline", underline);
    value.put("shadows", shadows);
    value.put("animation", "none");
    value.put("animationType", "entire");
    value.put("animationSpeed", "slow");
    return value;
  }
}
