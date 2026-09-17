package io.github.opendonationassistant.streamelements.overlay;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.events.widget.WidgetCommandSender;
import io.github.opendonationassistant.events.widget.WidgetCommandSender.WidgetUpdateCommand;
import io.github.opendonationassistant.streamelements.overlay.WidgetCreateClient.CreateWidgetRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Converts a StreamElements overlay JSON into an ODA `canvas` widget.
 *
 * <p>Flow: RPC `widget.create-request` creates the widget, then a
 * `WidgetUpdateCommand` (exchange `widgets`, key `command`) sets its
 * `elements` config property.
 */
@Singleton
public class OverlayWidgetImporter {

  public static final String WIDGET_TYPE = "canvas";
  public static final String ELEMENTS_PROPERTY = "elements";

  private final ODALogger log = new ODALogger(this);
  private final OverlayConverter converter;
  private final WidgetCreateClient createClient;
  private final WidgetCommandSender commandSender;

  @Inject
  public OverlayWidgetImporter(
    OverlayConverter converter,
    WidgetCreateClient createClient,
    WidgetCommandSender commandSender
  ) {
    this.converter = converter;
    this.createClient = createClient;
    this.commandSender = commandSender;
  }

  public OverlayWidgetImportResult createWidget(
    String overlayJson,
    String recipientId
  ) {
    var conversion = converter.convert(overlayJson);
    log.info(
      "Creating overlay widget",
      Map.of(
        "recipientId",
        recipientId,
        "widgetType",
        WIDGET_TYPE,
        "elements",
        conversion.elements().size(),
        "warnings",
        conversion.warnings()
      )
    );

    var created = createClient.create(
      new CreateWidgetRequest(WIDGET_TYPE, recipientId)
    );
    var property = new Widget.WidgetProperty(
      ELEMENTS_PROPERTY,
      ELEMENTS_PROPERTY,
      ELEMENTS_PROPERTY,
      conversion.elements()
    );
    commandSender.send(
      new WidgetUpdateCommand(
        created.id(),
        new Widget.WidgetConfig(List.of(property))
      )
    );
    return new OverlayWidgetImportResult(created.id(), conversion.warnings());
  }
}
