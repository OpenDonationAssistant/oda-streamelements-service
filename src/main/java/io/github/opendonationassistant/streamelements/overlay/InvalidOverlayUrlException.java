package io.github.opendonationassistant.streamelements.overlay;

public class InvalidOverlayUrlException extends IllegalArgumentException {

  public InvalidOverlayUrlException(String message) {
    super(message);
  }

  public InvalidOverlayUrlException(String message, Throwable cause) {
    super(message, cause);
  }
}
