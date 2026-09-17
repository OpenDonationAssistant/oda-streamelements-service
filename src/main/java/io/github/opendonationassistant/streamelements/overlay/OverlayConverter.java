package io.github.opendonationassistant.streamelements.overlay;

import com.fasterxml.uuid.Generators;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Deterministic StreamElements overlay scene → ODA element tree converter.
 *
 * <p>Implements `docs/streamelements-overlay-conversion.md`: a single
 * {@code fixed-coordinates-container} canvas root plus a flat, pre-order sorted
 * {@link OverlayElement} list. Conversions never throw on a single malformed
 * widget — unmappable widgets become placeholder labels and produce a
 * {@link ConversionWarning}.
 */
@Singleton
public class OverlayConverter {

  public static final String CANVAS_TYPE =
    ElementSettings.FIXED_COORDINATES_CONTAINER;
  public static final String WIDGET_NAME_FALLBACK = "Overlay";

  private static final String ROOT_NAME = "Canvas";
  private static final double DEFAULT_CANVAS_WIDTH = 1920d;
  private static final double DEFAULT_CANVAS_HEIGHT = 1080d;
  private static final double CANVAS_TOLERANCE = 1d;

  private final ObjectMapper mapper;

  @Inject
  public OverlayConverter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public OverlayConversion convert(String overlayJson) {
    var overlay = read(overlayJson);
    var warnings = new ArrayList<ConversionWarning>();
    var overlaySettings = JsonValues.map(overlay.get("settings"));
    var canvasWidth = OverlayGeometry.toNumber(
      overlaySettings.get("width"),
      DEFAULT_CANVAS_WIDTH
    );
    var canvasHeight = OverlayGeometry.toNumber(
      overlaySettings.get("height"),
      DEFAULT_CANVAS_HEIGHT
    );

    var rootId = newId();
    var positions = new LinkedHashMap<String, Object>();
    var rootSettings = ElementSettings.fixedCoordinatesContainer(
      canvasWidth,
      canvasHeight
    );
    rootSettings.put("positions", positions);

    var elements = new ArrayList<OverlayElement>();
    elements.add(
      new OverlayElement(
        rootId,
        ROOT_NAME,
        CANVAS_TYPE,
        true,
        null,
        0,
        false,
        0,
        rootSettings,
        0
      )
    );

    var order = new OrderCounter(1);
    for (var value : JsonValues.list(overlay.get("widgets"))) {
      var widget = SeWidget.from(value);
      if (
        applyBackgroundHeuristic(
          widget,
          canvasWidth,
          canvasHeight,
          rootSettings
        )
      ) {
        continue;
      }
      emit(
        mapWidget(widget, warnings),
        rootId,
        0,
        0,
        position(widget, warnings),
        elements,
        positions,
        order
      );
    }

    return new OverlayConversion(
      JsonValues.string(overlay.get("name"), WIDGET_NAME_FALLBACK),
      elements,
      warnings
    );
  }

  // ---------------------------------------------------------------- mapping

  private MappedWidget mapWidget(
    SeWidget widget,
    List<ConversionWarning> warnings
  ) {
    var mapped =
      switch (widget.type()) {
        case "image" -> media(widget, "image");
        case "video" -> media(widget, "video");
        case "text",
          "follower-latest",
          "subscriber-latest",
          "tip-latest",
          "se-widget-top-donation",
          "se-widget-countdown" -> label(widget, warnings);
        default -> placeholder(widget, warnings);
      };
    return wrapAnimations(widget, mapped, warnings);
  }

  private MappedWidget media(SeWidget widget, String mediaType) {
    var image = widget.image();
    var video = widget.video();
    var url = "video".equals(mediaType)
      ? JsonValues.string(video.get("src"), "")
      : JsonValues.string(image.get("src"), "");
    var css = widget.css();

    var settings = ElementSettings.media();
    settings.put("url", url);
    settings.put("type", mediaType);
    settings.put("name", mediaName(widget, url));
    settings.put(
      "opacity",
      OverlayGeometry.clamp(
        OverlayGeometry.toNumber(css.get("opacity"), 1d),
        0d,
        1d
      )
    );
    settings.put("width", mediaSize(css, "width", image));
    settings.put("height", mediaSize(css, "height", image));
    return template(widget, ElementSettings.MEDIA, settings);
  }

  private MappedWidget label(
    SeWidget widget,
    List<ConversionWarning> warnings
  ) {
    var text = widget.text();
    var textCss = JsonValues.map(text.get("css"));
    var css = widget.css();

    warnUnrepresentableText(widget, textCss, warnings);
    var shadows = shadows(widget, text, textCss, warnings);

    var settings = ElementSettings.label();
    settings.put("value", JsonValues.string(text.get("value"), ""));
    settings.put("font", font(textCss, shadows));
    settings.put("align", align(textCss.get("text-align")));
    settings.put("width", OverlayGeometry.toSize(css.get("width")));
    settings.put("height", OverlayGeometry.toSize(css.get("height")));
    return template(widget, ElementSettings.LABEL, settings);
  }

  private MappedWidget placeholder(
    SeWidget widget,
    List<ConversionWarning> warnings
  ) {
    warnings.add(
      ConversionWarning.of(
        widget,
        "no ODA element equivalent for se type '%s'".formatted(widget.type()),
        ConversionWarning.FALLBACK_PLACEHOLDER
      )
    );
    var css = widget.css();
    var settings = ElementSettings.label();
    settings.put(
      "value",
      "[%s] %s (не сконвертировано)".formatted(widget.type(), widget.name())
    );
    settings.put("width", OverlayGeometry.toSize(css.get("width")));
    settings.put("height", OverlayGeometry.toSize(css.get("height")));
    return template(widget, ElementSettings.LABEL, settings);
  }

  private MappedWidget wrapAnimations(
    SeWidget widget,
    MappedWidget mapped,
    List<ConversionWarning> warnings
  ) {
    var animation = widget.animation();
    var in = animationName(animation.get("in"));
    var out = animationName(animation.get("out"));
    warnTimeout(widget, animation, warnings);
    if (in.isBlank() && out.isBlank()) {
      return mapped;
    }
    var settings = ElementSettings.animations(
      resolveAnimation(widget, in, warnings),
      resolveAnimation(widget, out, warnings)
    );
    var template = new ElementTemplate(
      newId(),
      mapped.template().name() + " (animation)",
      ElementSettings.ANIMATIONS,
      mapped.template().enabled(),
      false,
      settings
    );
    return new MappedWidget(template, List.of(mapped));
  }

  // ---------------------------------------------------------------- emitting

  private void emit(
    MappedWidget mapped,
    String containerId,
    int parentLevel,
    int parentAdvancedLevel,
    @Nullable Map<String, Object> position,
    List<OverlayElement> out,
    Map<String, Object> positions,
    OrderCounter order
  ) {
    var template = mapped.template();
    var level = parentLevel + (template.advanced() ? 0 : 1);
    var advancedLevel = parentAdvancedLevel + 1;
    out.add(
      new OverlayElement(
        template.id(),
        template.name(),
        template.type(),
        template.enabled(),
        containerId,
        level,
        template.advanced(),
        advancedLevel,
        template.settings(),
        order.next()
      )
    );
    if (position != null) {
      positions.put(template.id(), position);
    }
    for (var child : mapped.children()) {
      emit(
        child,
        template.id(),
        level,
        advancedLevel,
        null,
        out,
        positions,
        order
      );
    }
  }

  // ---------------------------------------------------------------- helpers

  private boolean applyBackgroundHeuristic(
    SeWidget widget,
    double canvasWidth,
    double canvasHeight,
    Map<String, Object> rootSettings
  ) {
    if (!"image".equals(widget.type()) || !widget.locked()) {
      return false;
    }
    var css = widget.css();
    var coversCanvas =
      approximately(OverlayGeometry.toNumber(css.get("left"), -1d), 0d) &&
      approximately(OverlayGeometry.toNumber(css.get("top"), -1d), 0d) &&
      approximately(
        OverlayGeometry.toNumber(css.get("width"), -1d),
        canvasWidth
      ) &&
      approximately(
        OverlayGeometry.toNumber(css.get("height"), -1d),
        canvasHeight
      );
    var url = JsonValues.string(widget.image().get("src"), "");
    if (!coversCanvas || url.isBlank()) {
      return false;
    }
    var background = ElementSettings.image(widget.name(), url);
    background.put("size", "cover");
    rootSettings.put("backgroundImage", background);
    return true;
  }

  private Map<String, Object> position(
    SeWidget widget,
    List<ConversionWarning> warnings
  ) {
    var css = widget.css();
    warnNotNumeric(widget, css.get("left"), "css.left", warnings);
    warnNotNumeric(widget, css.get("top"), "css.top", warnings);
    var result = new LinkedHashMap<String, Object>();
    result.put(
      "x",
      OverlayGeometry.normalize(OverlayGeometry.toNumber(css.get("left"), 0d))
    );
    result.put(
      "y",
      OverlayGeometry.normalize(OverlayGeometry.toNumber(css.get("top"), 0d))
    );
    var zIndex = OverlayGeometry.zIndex(css.get("z-index"));
    if (zIndex != null) {
      result.put("zIndex", zIndex);
    }
    return result;
  }

  private static Map<String, Object> mediaSize(
    Map<String, Object> css,
    String key,
    Map<String, Object> image
  ) {
    var raw = css.get(key);
    if (raw != null && !"auto".equals(String.valueOf(raw))) {
      return OverlayGeometry.toSize(raw);
    }
    var imageCss = JsonValues.map(image.get("css"));
    var fallback = imageCss.get(key);
    if (fallback == null && "width".equals(key)) {
      fallback = imageCss.get("max-width");
    }
    return OverlayGeometry.toSize(fallback);
  }

  private static String mediaName(SeWidget widget, String url) {
    if (!widget.name().isBlank() || url.isBlank()) {
      return widget.name();
    }
    var withoutQuery = url.split("\\?")[0];
    var segments = withoutQuery.split("/");
    return segments.length == 0 ? widget.name() : segments[segments.length - 1];
  }

  private static Map<String, Object> font(
    Map<String, Object> textCss,
    List<Map<String, Object>> shadows
  ) {
    var family = JsonValues.string(textCss.get("font-family"), "Roboto");
    if (family.isBlank()) {
      family = "Roboto";
    }
    return ElementSettings.font(
      family,
      OverlayGeometry.toNumber(textCss.get("font-size"), 24d),
      JsonValues.string(textCss.get("color"), "#684aff"),
      isBold(textCss.get("font-weight")),
      "italic".equalsIgnoreCase(
          JsonValues.string(textCss.get("font-style"), "")
        ),
      JsonValues.string(textCss.get("text-decoration"), "").contains(
        "underline"
      ),
      shadows
    );
  }

  private static List<Map<String, Object>> shadows(
    SeWidget widget,
    Map<String, Object> text,
    Map<String, Object> textCss,
    List<ConversionWarning> warnings
  ) {
    if (!JsonValues.bool(text.get("enableShadow"), true)) {
      return List.of();
    }
    var parsed = OverlayGeometry.parseTextShadow(textCss.get("text-shadow"));
    if (parsed.usedFallback()) {
      warnings.add(
        ConversionWarning.of(
          widget,
          "text-shadow '%s' could not be parsed".formatted(
              JsonValues.string(textCss.get("text-shadow"), "")
            ),
          ConversionWarning.FALLBACK_PARTIAL
        )
      );
    }
    return parsed.shadows();
  }

  private static void warnUnrepresentableText(
    SeWidget widget,
    Map<String, Object> textCss,
    List<ConversionWarning> warnings
  ) {
    for (var key : List.of("text-transform", "line-height", "letter-spacing")) {
      if (textCss.get(key) != null) {
        warnings.add(
          ConversionWarning.of(
            widget,
            "'%s' has no ODA element equivalent".formatted(key),
            ConversionWarning.FALLBACK_PARTIAL
          )
        );
      }
    }
  }

  private static void warnTimeout(
    SeWidget widget,
    Map<String, Object> animation,
    List<ConversionWarning> warnings
  ) {
    if (OverlayGeometry.toNumber(animation.get("timeout"), 0d) != 0d) {
      warnings.add(
        ConversionWarning.of(
          widget,
          "animation.timeout is not representable in the elements system",
          ConversionWarning.FALLBACK_PARTIAL
        )
      );
    }
  }

  private static void warnNotNumeric(
    SeWidget widget,
    @Nullable Object value,
    String field,
    List<ConversionWarning> warnings
  ) {
    if (value != null && !OverlayGeometry.isNumeric(value)) {
      warnings.add(
        ConversionWarning.of(
          widget,
          "'%s' is not a number, defaulted to 0".formatted(field),
          ConversionWarning.FALLBACK_PARTIAL
        )
      );
    }
  }

  private static String resolveAnimation(
    SeWidget widget,
    String name,
    List<ConversionWarning> warnings
  ) {
    if (name.isBlank()) {
      return "none";
    }
    if (AnimationNames.isKnown(name)) {
      return name;
    }
    warnings.add(
      ConversionWarning.of(
        widget,
        "animation '%s' is not available in ODA".formatted(name),
        ConversionWarning.FALLBACK_PARTIAL
      )
    );
    return "none";
  }

  private static String animationName(@Nullable Object value) {
    var name = JsonValues.string(value, "").trim();
    return "none".equalsIgnoreCase(name) ? "" : name;
  }

  private static String align(@Nullable Object value) {
    var text = JsonValues.string(value, "left");
    return switch (text) {
      case "center", "right" -> text;
      default -> "left";
    };
  }

  private static boolean isBold(@Nullable Object value) {
    if (value instanceof Number number) {
      return number.doubleValue() >= 600d;
    }
    if (value instanceof String text) {
      if ("bold".equalsIgnoreCase(text.trim())) {
        return true;
      }
      try {
        return Integer.parseInt(text.trim()) >= 600;
      } catch (NumberFormatException exception) {
        return false;
      }
    }
    return false;
  }

  private static boolean approximately(double left, double right) {
    return Math.abs(left - right) <= CANVAS_TOLERANCE;
  }

  private Map<String, Object> read(String overlayJson) {
    try {
      var parsed = mapper.readValue(
        overlayJson,
        Argument.mapOf(String.class, Object.class)
      );
      return parsed == null ? Map.of() : parsed;
    } catch (IOException exception) {
      throw new IllegalArgumentException(
        "Overlay JSON could not be parsed",
        exception
      );
    }
  }

  private static String newId() {
    return Generators.timeBasedEpochGenerator().generate().toString();
  }

  private static MappedWidget template(
    SeWidget widget,
    String type,
    Map<String, Object> settings
  ) {
    return new MappedWidget(
      new ElementTemplate(
        newId(),
        widget.name(),
        type,
        widget.visible(),
        false,
        settings
      ),
      List.of()
    );
  }

  private record ElementTemplate(
    String id,
    String name,
    String type,
    boolean enabled,
    boolean advanced,
    Map<String, Object> settings
  ) {}

  private record MappedWidget(
    ElementTemplate template,
    List<MappedWidget> children
  ) {}

  private static final class OrderCounter {

    private int value;

    OrderCounter(int initial) {
      this.value = initial;
    }

    int next() {
      return value++;
    }
  }
}
