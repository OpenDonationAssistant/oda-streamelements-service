package io.github.opendonationassistant.streamelements.overlay;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates StreamElements text placeholders ({@code {name}}, {@code {amount}})
 * into ODA templating syntax ({@code <name>}, {@code <amount>}).
 *
 * <p>ODA resolves {@code <variable>} tokens against the widget's variable scope
 * (see the frontend {@code VariableStore}). {@link #ODA_NAMES} holds the
 * StreamElements → ODA variable name mapping; entries are currently an identity
 * mapping, so the name is preserved and only the delimiter changes. Fill in the
 * real ODA names here when they differ, without touching the converter.
 *
 * <p>Placeholders whose name is not in the dictionary are left untouched so
 * literal braces and unsupported variables survive the conversion verbatim.
 */
final class SeVariables {

  /** StreamElements variable name → ODA variable name. */
  private static final Map<String, String> ODA_NAMES = Map.ofEntries(
    // Variables documented in the overlay conversion spec.
    Map.entry("name", "name"),
    Map.entry("amount", "amount"),
    Map.entry("currency", "currency"),
    Map.entry("minutes", "minutes"),
    Map.entry("seconds", "seconds"),
    Map.entry("streamer", "streamer"),
    Map.entry("sender", "sender"),
    // Other common StreamElements text variables.
    Map.entry("message", "message"),
    Map.entry("username", "username"),
    Map.entry("tier", "tier"),
    Map.entry("count", "count"),
    Map.entry("total", "total"),
    Map.entry("goal", "goal"),
    Map.entry("title", "title"),
    Map.entry("viewers", "viewers"),
    Map.entry("followers", "followers"),
    Map.entry("subscribers", "subscribers")
  );

  private static final Pattern PLACEHOLDER = Pattern.compile(
    "\\{([A-Za-z0-9_.]+)\\}"
  );

  private SeVariables() {}

  static String toOda(String value) {
    var matcher = PLACEHOLDER.matcher(value);
    var result = new StringBuilder();
    while (matcher.find()) {
      var odaName = ODA_NAMES.get(matcher.group(1));
      var replacement = odaName == null
        ? matcher.group(0)
        : "<" + odaName + ">";
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
