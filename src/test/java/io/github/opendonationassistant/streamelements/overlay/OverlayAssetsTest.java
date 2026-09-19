package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.rabbit.RabbitClient;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverlayAssetsTest {

  private static final String FIRST =
    "https://cdn.streamelements.com/a.png";
  private static final String SECOND =
    "https://cdn.streamelements.com/b.png";
  private static final String THIRD =
    "https://cdn.streamelements.com/c.png";

  private final StreamElementsOverlayClient seClient = mock(
    StreamElementsOverlayClient.class
  );
  private final RabbitClient commandsFacade = mock(RabbitClient.class);
  private final ODALogger log = new ODALogger(this);

  @Test
  void rehost_preservesElementOrderAndRewritesStreamElementsAssets() {
    when(seClient.downloadAsset("se-token", FIRST)).thenReturn(new byte[] { 1 });
    when(seClient.downloadAsset("se-token", SECOND)).thenReturn(new byte[] { 2 });
    when(seClient.downloadAsset("se-token", THIRD)).thenReturn(new byte[] { 3 });
    var conversion = new OverlayConversion(
      "scene",
      List.of(
        media("element-1", FIRST),
        media("element-2", SECOND),
        media("element-3", THIRD)
      ),
      List.of()
    );

    var result = OverlayAssets.rehost(
      conversion,
      "user",
      "se-token",
      seClient,
      commandsFacade,
      log
    );

    assertEquals(
      List.of("element-1", "element-2", "element-3"),
      result.elements().stream().map(OverlayElement::id).toList()
    );
    assertTrue(
      result
        .elements()
        .stream()
        .allMatch(element ->
          String.valueOf(element.settings().get("url")).startsWith(
            "https://cdn.oda.digital/files/"
          )
        )
    );
    verify(commandsFacade, times(3)).sendCommand(any());
  }

  @Test
  void rehost_keepsOriginalUrlWhenDownloadFails() {
    var broken = "https://cdn.streamelements.com/broken.png";
    when(seClient.downloadAsset("se-token", broken)).thenThrow(
      new IllegalStateException("download failed")
    );
    var conversion = new OverlayConversion(
      "scene",
      List.of(media("element-1", broken)),
      List.of()
    );

    var result = OverlayAssets.rehost(
      conversion,
      "user",
      "se-token",
      seClient,
      commandsFacade,
      log
    );

    assertEquals(broken, result.elements().get(0).settings().get("url"));
  }

  @Test
  void rehost_leavesForeignAssetsUntouched() {
    var foreign = "https://cdn.example.com/asset.png";
    var conversion = new OverlayConversion(
      "scene",
      List.of(media("element-1", foreign)),
      List.of()
    );

    var result = OverlayAssets.rehost(
      conversion,
      "user",
      "se-token",
      seClient,
      commandsFacade,
      log
    );

    assertEquals(foreign, result.elements().get(0).settings().get("url"));
  }

  private static OverlayElement media(String id, String url) {
    var settings = new LinkedHashMap<String, Object>();
    settings.put("url", url);
    settings.put("type", "image");
    return new OverlayElement(
      id,
      id,
      "media",
      true,
      "root",
      1,
      false,
      1,
      settings,
      1
    );
  }
}
