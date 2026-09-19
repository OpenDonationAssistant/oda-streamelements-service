package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.widget.WidgetCommandSender;
import io.github.opendonationassistant.events.widget.WidgetCommandSender.WidgetUpdateCommand;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.streamelements.overlay.WidgetCreateClient.CreateWidgetRequest;
import io.github.opendonationassistant.streamelements.overlay.WidgetCreateClient.CreatedWidget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OverlayWidgetImporterTest {

  private static final String ASSET_URL =
    "https://cdn.streamelements.com/bar.png";

  private final OverlayConverter converter = mock(OverlayConverter.class);
  private final WidgetCreateClient createClient = mock(
    WidgetCreateClient.class
  );
  private final WidgetCommandSender widgetCommandSender = mock(
    WidgetCommandSender.class
  );
  private final StreamElementsOverlayClient client = mock(
    StreamElementsOverlayClient.class
  );
  private final RabbitClient commandsFacade = mock(RabbitClient.class);

  private final OverlayWidgetImporter importer = new OverlayWidgetImporter(
    converter,
    createClient,
    widgetCommandSender,
    client,
    commandsFacade
  );

  @Test
  void createWidget_rehostsStreamElementsAssetsThenSendsElementsCommand() {
    when(converter.convert("{}")).thenReturn(
      new OverlayConversion("scene", List.of(media(ASSET_URL)), List.of())
    );
    when(client.resolveToken("user", "token-1")).thenReturn("se-token");
    when(client.downloadAsset("se-token", ASSET_URL)).thenReturn(
      new byte[] { 1, 2 }
    );
    when(createClient.create(any())).thenReturn(createdWidget());

    var result = importer.createWidget("{}", "user", "token-1");

    var uploadCaptor = ArgumentCaptor.forClass(
      OverlayAssets.UploadFileCommand.class
    );
    verify(commandsFacade).sendCommand(uploadCaptor.capture());
    var upload = uploadCaptor.getValue();
    assertEquals("user", upload.recipientId());
    assertArrayEquals(new byte[] { 1, 2 }, upload.content());
    assertTrue(upload.filename().endsWith(".png"));

    verify(createClient).create(new CreateWidgetRequest("canvas", "user"));
    var updateCaptor = ArgumentCaptor.forClass(WidgetUpdateCommand.class);
    verify(widgetCommandSender).send(updateCaptor.capture());
    var elements = elementsOf(updateCaptor.getValue());
    assertEquals(
      "https://cdn.oda.digital/files/" + upload.filename(),
      elements.get(0).settings().get("url")
    );
    assertEquals("widget-1", result.widgetId());
  }

  @Test
  void createWidget_leavesNonStreamElementsAssetsUntouched() {
    var foreignUrl = "https://cdn.example.com/bar.png";
    when(converter.convert("{}")).thenReturn(
      new OverlayConversion("scene", List.of(media(foreignUrl)), List.of())
    );
    when(client.resolveToken("user", "token-1")).thenReturn("se-token");
    when(createClient.create(any())).thenReturn(createdWidget());

    importer.createWidget("{}", "user", "token-1");

    verifyNoInteractions(commandsFacade);
    verify(client, never()).downloadAsset(any(), any());
    var updateCaptor = ArgumentCaptor.forClass(WidgetUpdateCommand.class);
    verify(widgetCommandSender).send(updateCaptor.capture());
    assertEquals(
      foreignUrl,
      elementsOf(updateCaptor.getValue()).get(0).settings().get("url")
    );
  }

  @Test
  void createWidget_keepsOriginalUrlWhenAssetDownloadFails() {
    when(converter.convert("{}")).thenReturn(
      new OverlayConversion("scene", List.of(media(ASSET_URL)), List.of())
    );
    when(client.resolveToken("user", "token-1")).thenReturn("se-token");
    when(client.downloadAsset("se-token", ASSET_URL)).thenThrow(
      new IllegalStateException("download failed")
    );
    when(createClient.create(any())).thenReturn(createdWidget());

    importer.createWidget("{}", "user", "token-1");

    verifyNoInteractions(commandsFacade);
    var updateCaptor = ArgumentCaptor.forClass(WidgetUpdateCommand.class);
    verify(widgetCommandSender).send(updateCaptor.capture());
    assertEquals(
      ASSET_URL,
      elementsOf(updateCaptor.getValue()).get(0).settings().get("url")
    );
  }

  @SuppressWarnings("unchecked")
  private static List<OverlayElement> elementsOf(WidgetUpdateCommand command) {
    var value = command.patch().properties().get(0).value();
    return (List<OverlayElement>) java.util.Objects.requireNonNull(value);
  }

  private static OverlayElement media(String url) {
    var settings = new LinkedHashMap<String, Object>();
    settings.put("url", url);
    settings.put("type", "image");
    return new OverlayElement(
      "element-1",
      "BAR",
      "media",
      true,
      "root-id",
      1,
      false,
      1,
      settings,
      1
    );
  }

  private static CreatedWidget createdWidget() {
    return new CreatedWidget(
      "widget-1",
      "canvas",
      0,
      "canvas",
      "user",
      Map.of(),
      true,
      false,
      List.of()
    );
  }
}
