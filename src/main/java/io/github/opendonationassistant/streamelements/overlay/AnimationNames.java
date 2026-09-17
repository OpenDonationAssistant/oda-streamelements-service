package io.github.opendonationassistant.streamelements.overlay;

import java.util.Set;

/**
 * The animate.css names ODA accepts, mirrored from the frontend
 * {@code ALL_ANIMATIONS} (APPEARANCE + IDLE + OUT). Names outside this set
 * cannot be rendered by ODA and are replaced with {@code "none"}.
 */
final class AnimationNames {

  private static final Set<String> ALL = Set.of(
    // appearance
    "bounce", "flash", "pulse", "rubberBand", "shakeY", "shakeX", "headShake",
    "swing", "tada", "wobble", "jello", "heartBeat", "jackInTheBox",
    "backInDown", "backInLeft", "backInRight", "backInUp", "bounceIn",
    "bounceInDown", "bounceInLeft", "bounceInRight", "bounceInUp", "fadeIn",
    "fadeInDown", "fadeInDownBig", "fadeInLeft", "fadeInLeftBig",
    "fadeInRight", "fadeInRightBig", "fadeInUp", "fadeInUpBig",
    "fadeInTopLeft", "fadeInTopRight", "fadeInBottomLeft",
    "fadeInBottomRight", "flip", "flinInX", "flipInY", "lightSpeedInRight",
    "lightSpeedInLeft", "rotateIn", "rotateInDownLeft", "rotateInDownRight",
    "rotateInUpLeft", "rotateInUpRight", "hinge", "rollIn", "zoomIn",
    "zoomInDown", "zoomInLeft", "zoomInRight", "zoomInUp", "slideInDown",
    "slideInLeft", "slideInRight", "slideInUp",
    // out
    "backOutDown", "backOutLeft", "backOutRight", "backOutUp", "bounceOut",
    "bounceOutDown", "bounceOutLeft", "bounceOutRight", "bounceOutUp",
    "fadeOut", "fadeOutDown", "fadeOutDownBig", "fadeOutLeft",
    "fadeOutLeftBig", "fadeOutRight", "fadeOutRightBig", "fadeOutUp",
    "fadeOutUpBig", "fadeOutTopLeft", "fadeOutTopRight",
    "fadeOutBottomRight", "fadeOutBottomLeft", "flipOutX", "flipOutY",
    "lightSpeedOutRight", "lightSpeedOutLeft", "rotateOut",
    "rotateOutDownLeft", "rotateOutDownRight", "rotateOutUpLeft",
    "rotateOutUpRight", "rollOut", "zoomOut", "zoomOutDown", "zoomOutLeft",
    "zoomOutRight", "zoomOutUp", "slideOutDown", "slideOutLeft",
    "slideOutRight", "slideOutUp"
  );

  private AnimationNames() {}

  static boolean isKnown(String name) {
    return ALL.contains(name);
  }
}
