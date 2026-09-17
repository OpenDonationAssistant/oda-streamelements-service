package io.github.opendonationassistant.streamelements.overlay;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

/**
 * Result of converting a StreamElements overlay scene: the widget name and the
 * flat, pre-order sorted element tree plus the conversion report.
 */
@Serdeable
public record OverlayConversion(
  String name,
  List<OverlayElement> elements,
  List<ConversionWarning> warnings
) {}
