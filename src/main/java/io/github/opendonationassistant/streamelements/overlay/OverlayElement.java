package io.github.opendonationassistant.streamelements.overlay;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A single node of the flat {@code ElementData[]} tree stored under the
 * {@code elements} widget config property. Parent/child relations are expressed
 * by {@link #containerId}, render order by {@link #order}.
 */
@Serdeable
public record OverlayElement(
  String id,
  String name,
  String type,
  boolean enabled,
  @Nullable String containerId,
  int level,
  boolean advanced,
  int advancedLevel,
  Map<String, Object> settings,
  int order
) {}
