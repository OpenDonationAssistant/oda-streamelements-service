package io.github.opendonationassistant.streamelements.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.widget.WidgetCommandSender;
import io.github.opendonationassistant.events.widget.WidgetCommandSender.WidgetUpdateCommand;
import io.github.opendonationassistant.streamelements.overlay.WidgetCreateClient.CreateWidgetRequest;
import io.github.opendonationassistant.streamelements.overlay.WidgetCreateClient.CreatedWidget;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OverlayWidgetImporterTest {

  private final OverlayConverter converter = mock(OverlayConverter.class);
  private final WidgetCreateClient createClient = mock(WidgetCreateClient.class);
  private final WidgetCommandSender commandSender = mock(
    WidgetCommandSender.class
  );

  private final OverlayWidgetImporter importer = new OverlayWidgetImporter(
    converter,
    createClient,
    commandSender
  );

  @Test
  void createWidget_createsCanvasWidgetThenSendsElementsCommand() {
    var element = new OverlayElement(
      "element-1",
      "BAR",
      "media",
      true,
      "root-id",
      1,
      false,
      1,
      Map.of(),
      1
    );
    var warning = new ConversionWarning(
      "25",
      "se-widget-twitch-chat",
      "CHAT",
      "no ODA element equivalent",
      "placeholder"
    );
    when(converter.convert("{}")).thenReturn(
      new OverlayConversion("scene", List.of(element), List.of(warning))
    );
    when(createClient.create(any())).thenReturn(
      new CreatedWidget(
        "widget-1",
        "canvas",
        0,
        "canvas",
        "user",
        Map.of(),
        true,
        false,
        List.of()
      )
    );

    var result = importer.createWidget("{}", "user");

    verify(createClient).create(new CreateWidgetRequest("canvas", "user"));
    var captor = ArgumentCaptor.forClass(WidgetUpdateCommand.class);
    verify(commandSender).send(captor.capture());

    var command = captor.getValue();
    assertEquals("widget-1", command.id());
    assertEquals(1, command.patch().properties().size());
    var property = command.patch().properties().get(0);
    assertEquals("elements", property.name());
    assertEquals(List.of(element), property.value());
    assertEquals("widget-1", result.widgetId());
    assertEquals(List.of(warning), result.warnings());
  }
}
