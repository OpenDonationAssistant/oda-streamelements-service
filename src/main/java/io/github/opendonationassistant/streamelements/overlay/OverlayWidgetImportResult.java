package io.github.opendonationassistant.streamelements.overlay;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

/** Outcome of importing an overlay into a newly created ODA widget. */
@Serdeable
public record OverlayWidgetImportResult(
  String widgetId,
  List<ConversionWarning> warnings
) {}
