package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeOverlayUrlTest {

  @Test
  void parse_returnsOverlayIdFromOverlayUrl() {
    assertEquals(
      "658471962c16b1688cdd2203",
      SeOverlayUrl.parse(
        "https://streamelements.com/overlay/658471962c16b1688cdd2203/SPtp541QPP1OOLkpV6MK3Amm8nlZwMxeGAUPI4RneUKW9i5l"
      )
    );
  }

  @Test
  void parse_ignoresQueryStringAndFragment() {
    assertEquals(
      "abc123",
      SeOverlayUrl.parse(
        "https://streamelements.com/overlay/abc123/token?x=1#frag"
      )
    );
  }

  @Test
  void parse_rejectsForeignHost() {
    assertThrows(
      InvalidOverlayUrlException.class,
      () -> SeOverlayUrl.parse("https://evil.com/overlay/abc123/token")
    );
  }

  @Test
  void parse_rejectsMissingOverlayPrefix() {
    assertThrows(
      InvalidOverlayUrlException.class,
      () -> SeOverlayUrl.parse("https://streamelements.com/abc123/token")
    );
  }

  @Test
  void parse_rejectsMissingTokenSegment() {
    assertThrows(
      InvalidOverlayUrlException.class,
      () -> SeOverlayUrl.parse("https://streamelements.com/overlay/abc123")
    );
  }

  @Test
  void parse_rejectsBlankAndNull() {
    assertThrows(InvalidOverlayUrlException.class, () -> SeOverlayUrl.parse(""));
    assertThrows(InvalidOverlayUrlException.class, () -> SeOverlayUrl.parse("   "));
    assertThrows(InvalidOverlayUrlException.class, () -> SeOverlayUrl.parse(null));
  }
}
