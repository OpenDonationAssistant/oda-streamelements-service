package io.github.opendonationassistant.streamelements.overlay;

import java.util.Map;

/** A StreamElements scene widget plus the normalized fields the converter needs. */
record SeWidget(String id, String type, String name, Map<String, Object> raw) {
  private static final String FALLBACK_NAME = "element";

  static SeWidget from(Object value) {
    var raw = JsonValues.map(value);
    var type = JsonValues.string(raw.get("type"), "");
    var name = JsonValues.string(
      raw.get("name"),
      type.isBlank() ? FALLBACK_NAME : type
    );
    return new SeWidget(
      JsonValues.string(raw.get("id"), ""),
      type,
      name,
      raw
    );
  }

  Map<String, Object> css() {
    return JsonValues.map(raw.get("css"));
  }

  Map<String, Object> text() {
    return JsonValues.map(raw.get("text"));
  }

  Map<String, Object> image() {
    return JsonValues.map(raw.get("image"));
  }

  Map<String, Object> video() {
    return JsonValues.map(raw.get("video"));
  }

  Map<String, Object> animation() {
    return JsonValues.map(raw.get("animation"));
  }

  boolean visible() {
    return JsonValues.bool(raw.get("visible"), true);
  }

  boolean locked() {
    return JsonValues.bool(raw.get("locked"), false);
  }
}
