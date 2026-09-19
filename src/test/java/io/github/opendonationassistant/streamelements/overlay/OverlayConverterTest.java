package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.serde.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class OverlayConverterTest {

  private final OverlayConverter converter = new OverlayConverter(
    ObjectMapper.getDefault()
  );

  private static final String SCENE =
    """
    {
      "settings": { "width": 1920, "height": 1080, "name": "1080p" },
      "name": "RED FLARE START SCENE",
      "type": "super",
      "widgets": [
        { "id": 1, "type": "image", "name": "BACKGROUND", "visible": true, "locked": true,
          "css": { "z-index": 2, "top": "0px", "left": "0px", "width": "1920px", "height": "1080px" },
          "image": { "src": "https://cdn/background.png" } },
        { "id": 2, "type": "image", "name": "BAR", "visible": true, "locked": false,
          "css": { "z-index": 3, "top": "47.34px", "left": "731.00px", "width": "262.39px", "height": "83.85px", "opacity": 1 },
          "image": { "src": "https://cdn/bar.png" } },
        { "id": 13, "type": "text", "name": "/FACEBOOK", "visible": true,
          "css": { "z-index": 17, "top": "263", "left": "1651.76", "width": "auto", "height": "auto" },
          "text": { "value": "/FACEBOOK", "enableShadow": true,
            "css": { "font-family": "Righteous", "font-size": 15, "color": "#fefffe",
                     "font-weight": "normal", "text-align": "center",
                     "text-shadow": "rgb(0, 0, 0) 1px 1px 1px" } } },
        { "id": 20, "type": "text", "name": "ANIM", "visible": true,
          "css": { "top": "10px", "left": "20px", "width": "100%", "height": "50px" },
          "text": { "value": "hello", "css": { "font-size": 30, "font-weight": "bold" } },
          "animation": { "in": "fadeIn", "out": "fadeOut", "timeout": 6 } },
        { "id": 25, "type": "se-widget-twitch-chat", "name": "CHAT", "visible": false,
          "css": { "top": "5px", "left": "5px", "width": "100px", "height": "40px" } },
        { "id": 30, "type": "text", "name": "BADANIM", "visible": true,
          "css": { "top": "1px", "left": "2px" },
          "text": { "value": "x", "css": {} },
          "animation": { "in": "notAnAnimation" } },
        { "id": 40, "type": "video", "name": "VID", "visible": true,
          "css": { "top": "3px", "left": "4px", "width": "320px", "height": "240px" },
          "video": { "src": "https://cdn/v.mp4" } }
      ]
    }
    """;

  @Test
  void convert_buildsCanvasRootWithSizesAndNormalizedPositions() {
    var conversion = converter.convert(SCENE);

    assertEquals("RED FLARE START SCENE", conversion.name());
    var root = conversion.elements().get(0);
    assertEquals("fixed-coordinates-container", root.type());
    assertEquals(0, root.order());
    assertNull(root.containerId());
    assertEquals(mapOf("type", "fixed", "value", 1920), root.settings().get("width"));
    assertEquals(mapOf("type", "fixed", "value", 1080), root.settings().get("height"));

    var positions = positions(root);
    var bar = byName(conversion, "BAR");
    assertEquals(mapOf("x", 731, "y", 47.34, "zIndex", 3), positions.get(bar.id()));
  }

  @Test
  void convert_emitsFlatPreOrderListAscendingByOrder() {
    var conversion = converter.convert(SCENE);

    var orders = conversion.elements().stream().map(OverlayElement::order).toList();
    assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8), orders);

    var ids = conversion.elements().stream()
      .map(OverlayElement::id)
      .collect(Collectors.toSet());
    conversion.elements().stream()
      .filter(element -> element.containerId() != null)
      .forEach(element -> assertTrue(ids.contains(element.containerId())));
  }

  @Test
  void convert_mapsImageToMediaWithUrlAndSize() {
    var bar = byName(converter.convert(SCENE), "BAR");

    assertEquals("media", bar.type());
    assertEquals("https://cdn/bar.png", bar.settings().get("url"));
    assertEquals("image", bar.settings().get("type"));
    assertEquals("BAR", bar.settings().get("name"));
    assertEquals(mapOf("type", "fixed", "value", 262.39), bar.settings().get("width"));
    assertEquals(mapOf("type", "fixed", "value", 83.85), bar.settings().get("height"));
  }

  @Test
  void convert_mapsVideoToMediaOfTypeVideo() {
    var vid = byName(converter.convert(SCENE), "VID");

    assertEquals("media", vid.type());
    assertEquals("video", vid.settings().get("type"));
    assertEquals("https://cdn/v.mp4", vid.settings().get("url"));
  }

  @Test
  void convert_mapsTextToLabelWithFontAndAlign() {
    var label = byName(converter.convert(SCENE), "/FACEBOOK");

    assertEquals("label", label.type());
    assertEquals("/FACEBOOK", label.settings().get("value"));
    assertEquals("center", label.settings().get("align"));
    assertEquals(mapOf("type", "min", "value", 0), label.settings().get("width"));

    var font = asMap(label.settings().get("font"));
    assertEquals("Righteous", font.get("family"));
    assertEquals(15, font.get("size"));
    assertEquals(false, font.get("weight"));
    assertEquals(false, font.get("italic"));
    assertEquals(false, font.get("underline"));
    var shadows = asList(font.get("shadows"));
    assertEquals(1, shadows.size());
    assertEquals("rgb(0, 0, 0)", shadows.get(0).get("color"));
    assertEquals(1, shadows.get(0).get("x"));
  }

  @Test
  void convert_translatesStreamElementsPlaceholdersToOdaSyntax() {
    var conversion = converter.convert(
      """
      { "widgets": [ { "id": 1, "type": "text", "name": "TIP",
        "css": {}, "text": { "value": "{name} donated {amount} {currency}" } } ] }
      """
    );

    assertEquals(
      "<name> donated <amount> <currency>",
      byName(conversion, "TIP").settings().get("value")
    );
  }

  @Test
  void convert_translatesCountdownPlaceholders() {
    var conversion = converter.convert(
      """
      { "widgets": [ { "id": 1, "type": "se-widget-countdown", "name": "CD",
        "css": {}, "text": { "value": "{minutes}:{seconds}" } } ] }
      """
    );

    assertEquals(
      "<minutes>:<seconds>",
      byName(conversion, "CD").settings().get("value")
    );
  }

  @Test
  void convert_leavesUnknownAndUnclosedPlaceholdersUntouched() {
    var conversion = converter.convert(
      """
      { "widgets": [ { "id": 1, "type": "text", "name": "RAW",
        "css": {}, "text": { "value": "{unknown} {name" } } ] }
      """
    );

    assertEquals(
      "{unknown} {name",
      byName(conversion, "RAW").settings().get("value")
    );
  }

  @Test
  void convert_wrapsAnimationsWhenPresent() {
    var conversion = converter.convert(SCENE);

    var wrapper = byName(conversion, "ANIM (animation)");
    assertEquals("animations", wrapper.type());
    var inner = byName(conversion, "ANIM");
    assertEquals(wrapper.id(), inner.containerId());

    var settings = wrapper.settings();
    assertEquals(mapOf("animation", "fadeIn", "duration", 1000), settings.get("inAnimation"));
    assertEquals(mapOf("animation", "fadeOut", "duration", 1000), settings.get("outAnimation"));
    assertTrue(wrapper.order() < inner.order());
  }

  @Test
  void convert_fallsBackToNoneForUnknownAnimation() {
    var conversion = converter.convert(SCENE);

    var wrapper = byName(conversion, "BADANIM (animation)");
    assertEquals("animations", wrapper.type());
    assertEquals(
      mapOf("animation", "none", "duration", 1000),
      wrapper.settings().get("inAnimation")
    );
    assertTrue(
      conversion.warnings().stream().anyMatch(warning ->
        warning.reason().contains("notAnAnimation")
      )
    );
  }

  @Test
  void convert_acceptsFlipInXAnimation() {
    var conversion = converter.convert(
      """
      { "widgets": [ { "id": 1, "type": "text", "name": "F",
        "css": {}, "text": { "value": "v" },
        "animation": { "in": "flipInX" } } ] }
      """
    );

    var wrapper = byName(conversion, "F (animation)");
    assertEquals(
      mapOf("animation", "flipInX", "duration", 1000),
      wrapper.settings().get("inAnimation")
    );
    assertFalse(
      conversion.warnings().stream().anyMatch(warning ->
        warning.reason().contains("flipInX")
      )
    );
  }

  @Test
  void convert_emitsPlaceholderAndWarningForUnmappableWidget() {
    var conversion = converter.convert(SCENE);

    var placeholder = byName(conversion, "CHAT");
    assertEquals("label", placeholder.type());
    assertFalse(placeholder.enabled());
    assertTrue(
      String.valueOf(placeholder.settings().get("value")).startsWith(
        "[se-widget-twitch-chat] CHAT"
      )
    );
    assertTrue(
      conversion.warnings().stream().anyMatch(warning ->
        "se-widget-twitch-chat".equals(warning.seType()) &&
        "placeholder".equals(warning.fallback())
      )
    );
  }

  @Test
  void convert_usesLockedFullCanvasImageAsRootBackground() {
    var conversion = converter.convert(SCENE);

    var root = conversion.elements().get(0);
    var backgroundImage = asMap(root.settings().get("backgroundImage"));
    assertEquals("BACKGROUND", backgroundImage.get("name"));
    assertEquals("https://cdn/background.png", backgroundImage.get("url"));
    assertTrue(
      conversion.elements().stream().noneMatch(element ->
        "BACKGROUND".equals(element.name())
      )
    );
  }

  @Test
  void convert_defaultsCanvasWhenSettingsMissing() {
    var conversion = converter.convert("{\"widgets\": []}");

    var root = conversion.elements().get(0);
    assertEquals(mapOf("type", "fixed", "value", 1920), root.settings().get("width"));
    assertEquals(mapOf("type", "fixed", "value", 1080), root.settings().get("height"));
    assertEquals(1, conversion.elements().size());
  }

  @Test
  void convert_recordsWarningForUnparseablePosition() {
    var conversion = converter.convert(
      """
      { "settings": { "width": 640, "height": 480 },
        "widgets": [ { "id": 7, "type": "text", "name": "T",
          "css": { "left": "oops", "top": "oops" },
          "text": { "value": "v" } } ] }
      """
    );

    var element = byName(conversion, "T");
    assertEquals(mapOf("x", 0, "y", 0), positions(conversion.elements().get(0)).get(element.id()));
    assertTrue(
      conversion.warnings().stream().anyMatch(warning ->
        warning.reason().contains("css.left")
      )
    );
  }

  @Test
  void convert_fallsBackWhenTextShadowUnparseable() {
    var conversion = converter.convert(
      """
      { "widgets": [ { "id": 9, "type": "text", "name": "S",
        "css": {}, "text": { "value": "v", "enableShadow": true,
        "css": { "text-shadow": "not-a-shadow" } } } ] }
      """
    );

    var font = asMap(byName(conversion, "S").settings().get("font"));
    var shadows = asList(font.get("shadows"));
    assertEquals("rgb(0, 0, 0)", shadows.get(0).get("color"));
    assertEquals(1, shadows.get(0).get("blur"));
    assertTrue(
      conversion.warnings().stream().anyMatch(warning ->
        warning.reason().contains("text-shadow")
      )
    );
  }

  @Test
  void convert_rejectsInvalidJson() {
    assertThrows(
      IllegalArgumentException.class,
      () -> converter.convert("{ not json")
    );
  }

  private static OverlayElement byName(
    OverlayConversion conversion,
    String name
  ) {
    return conversion
      .elements()
      .stream()
      .filter(element -> name.equals(element.name()))
      .findFirst()
      .orElseThrow(() ->
        new AssertionError("Element not found: " + name)
      );
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> positions(OverlayElement root) {
    return (Map<String, Object>) Objects.requireNonNull(
      root.settings().get("positions")
    );
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(@Nullable Object value) {
    return (Map<String, Object>) Objects.requireNonNull(value);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> asList(@Nullable Object value) {
    return (List<Map<String, Object>>) Objects.requireNonNull(value);
  }

  private static Map<String, Object> mapOf(Object... keyValues) {
    var result = new java.util.LinkedHashMap<String, Object>();
    for (var index = 0; index < keyValues.length; index += 2) {
      result.put((String) keyValues[index], keyValues[index + 1]);
    }
    return result;
  }
}
