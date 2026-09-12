# listener/ — RabbitMQ Consumers + Event Handlers

Parent: root `AGENTS.md` (build, static-analysis, and platform rules live there).

## OVERVIEW
Two inbound styles coexist: `EventsListener` (raw dispatch to a shared `MessageProcessor`) and `WidgetChangesEventListener` (typed deserialization). `handler/` holds typed `AbstractMessageHandler<T>` beans discovered by type name.

## LISTENERS
| Class | Queue | Executor | Binding |
|-------|-------|----------|---------|
| `EventsListener` | `streamelements.events` | `config-listener` | `history` → `event.HistoryItemEvent`; `twitch` → `event.TwitchChannelFollowEvent`, `event.TwitchChannelSubscriptionMessageEvent`, `event.TwitchChannelRaidEvent` |
| `WidgetChangesEventListener` | `streamelements.config` | `event-listener` | `changes.widgets` (`*`) |

- `EventsListener` is **raw**: method takes `@MessageHeader("type") String`, `byte[] payload`, `RabbitAcknowledgement ack`, and forwards to `MessageProcessor.process(...)`. It does not deserialize.
- `WidgetChangesEventListener` is **typed**: method takes a deserialized `WidgetChangedEvent`, null-guards `widget`/`id`/`config`/`properties`, then `repository.getSession(ownerId).thenAccept(session -> session.apply(widget)).join()`.

## BINDING REGISTRATION (critical)
Bindings are **not** auto-discovered. `Application.rabbitConfiguration()` builds `AMQPConfiguration` from `WidgetChangesEventListener.BINDING` + `EventsListener.BINDING`. A new queue/binding must be added there, or it never gets declared.

## HANDLER DISPATCH
- Handlers live in `handler/`, are `@Singleton`, and `extend AbstractMessageHandler<T>` (`io.github.opendonationassistant.events.*` — from the external `oda-*` jars, not this repo).
- Constructor signature: `@Inject XHandler(ObjectMapper mapper, StreamElementsSessionRepository repository)` → `super(mapper)`.
- `MessageProcessor` selects the handler whose `type()` equals the incoming `type` header. `type()` = **simple class name of the generic parameter `T`** (e.g. `TwitchChannelRaidEvent`).
- To add an event type: create `@Singleton class XHandler extends AbstractMessageHandler<XEvent>` — no explicit registration needed. Add the queue binding in `Application` only if the event needs a new exchange route.
- Handlers null-guard `recipientId` (and key payload fields) with early `return`, then `repository.getSession(recipientId).join().setXLatest(...)`.

## CONVENTIONS
- Queue constants: `public static final String QUEUE_NAME`, `public static final Queue QUEUE = new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME)`, `public static final Exchange BINDING` (or `List<Exchange>`).
- Use the fully-qualified `io.github.opendonationassistant.rabbit.Queue` to avoid clashing with Micronaut's `@Queue` annotation import.
- `@RabbitListener(executor = ...)` on the class; `@Queue(QUEUE_NAME)` on the method.
- Naming is inconsistent: `TwitchFollowEventHandler` carries an `Event` suffix; `TwitchRaidHandler` / `TwitchSubscriptionHandler` / `HistoryItemEventHandler` do not.

## ANTI-PATTERNS / GOTCHAS
- Forgetting to register a new `BINDING` in `Application.rabbitConfiguration()` makes the listener silently receive nothing.
- All listener executors are `fixed` `nThreads: 1` (`application.yml`); handlers block with `.join()`, so a slow handler stalls its whole queue.
- `EventsListener` uses the `config-listener` pool despite its queue name, and injects `StreamElementsSessionRepository` that it never uses (dead injection).
- The `command-listener` executor is declared but unused.
- No tests exist for `EventsListener` or any of the four handlers.
- `AbstractMessageHandler` / `MessageProcessor` / event records come from `oda-rabbit-conf` (which transitively brings `oda-commons`); they are not in this repo.
