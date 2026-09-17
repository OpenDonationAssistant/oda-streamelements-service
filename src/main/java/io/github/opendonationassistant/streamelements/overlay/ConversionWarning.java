package io.github.opendonationassistant.streamelements.overlay;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ConversionWarning(
  String seWidgetId,
  String seType,
  String seName,
  String reason,
  String fallback
) {
  public static final String FALLBACK_PLACEHOLDER = "placeholder";
  public static final String FALLBACK_PARTIAL = "partial";
  public static final String FALLBACK_SKIPPED = "skipped";

  static ConversionWarning of(SeWidget widget, String reason, String fallback) {
    return new ConversionWarning(
      widget.id(),
      widget.type(),
      widget.name(),
      reason,
      fallback
    );
  }
}
