# AGENTS.md

**Generated:** 2026-09-12
**Commit:** 0fe4b2f (`main`)
**Mode:** regenerated (previous tracked AGENTS.md was deleted in the working tree)

## OVERVIEW
Micronaut 5.0.2 / Java 25 Maven service (AGPL-3.0) in the Open Donation Assistant platform. Emulates the StreamElements API for streamers: consumes donation/twitch events from RabbitMQ, keeps per-recipient session state in an embedded Infinispan cache, and pushes tip/follow/subscriber/raid updates to streamer widgets over RabbitMQ. **This is Micronaut, not Spring Boot.**

## STRUCTURE
```
oda-streamelements-service/
├── src/main/java/io/github/opendonationassistant/
│   ├── Application.java                 # @Factory: main() + Infinispan + Rabbit bindings + @Named("commands") client
│   ├── SerdeableEntryMarshaller.java    # Infinispan marshaller — UNREFERENCED (dead code)
│   └── streamelements/
│       ├── WidgetFacade.java            # outbound UI publisher to amq.topic (nested @RabbitClient)
│       ├── WidgetConfigClient.java      # @RabbitClient RPC to widget.config-request
│       ├── StreamElementsStateCacheConfiguration.java  # creates the Infinispan Map bean
│       ├── listener/                    # Rabbit consumers + handlers  (see listener/AGENTS.md)
│       ├── repository/                  # session state model        (see repository/AGENTS.md)
│       └── view/                        # HTTP controllers + @Serdeable DTOs
├── src/test/java/...                    # JUnit 5 + @MicronautTest + Instancio + Mockito
├── src/main/resources/                  # application.yml, application-standalone.yml, logback.xml
├── .mvn/jvm.config                      # REQUIRED ErrorProne --add-exports/--add-opens flags
├── aot-jar.properties / aot-native-image.properties
├── Dockerfile                           # runs the native binary target/oda-streamelements-service
└── pom.xml
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add/adjust inbound event | `streamelements/listener/` | see `listener/AGENTS.md` |
| Change session state | `streamelements/repository/` | see `repository/AGENTS.md` |
| Widget outbound payload | `streamelements/WidgetFacade.java` | `sendEvent` → `{recipientId}.streamelements` on `amq.topic` |
| Widget config lookup | `streamelements/WidgetConfigClient.java` | RPC, reply-to `amq.rabbitmq.reply-to` |
| HTTP endpoint | `streamelements/view/` | controllers return `CompletableFuture<HttpResponse<...>>` |
| Cache / infra beans | `Application.java`, `StreamElementsStateCacheConfiguration.java` | |
| Static-analysis rules | `pom.xml` `compilerArgs` | NullAway/ErrorProne |
| CI / release | `.github/workflows/maven.yml` | delegates to `oda-libraries` reusable workflow |

## CODE MAP
| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `Application` | class / `@Factory` | `Application.java:37` | bootstrap + Infinispan managers + Rabbit `AMQPConfiguration` |
| `StreamElementsSessionRepository` | class | `repository/…:13` | load-or-create session; lazy widget-config hydrate |
| `StreamElementsSession` | class | `repository/…:14` | mutable aggregate; `save()` + publish UI event |
| `StreamElementsData` | record | `repository/…:7` | immutable persisted state (`withX` copies) |
| `StreamElementsDataRepository` | class | `repository/…:10` | thin wrapper over Infinispan `Map` bean |
| `WidgetFacade` | class | `streamelements/…:16` | outbound event publisher |
| `WidgetConfigClient` | interface | `streamelements/…:13` | outbound config RPC |
| `EventsListener` | class | `listener/…:16` | raw dispatch to `MessageProcessor` |
| `WidgetChangesEventListener` | class | `listener/…:13` | typed widget-config updates |
| `TwitchFollowEventHandler` / `TwitchSubscriptionHandler` / `TwitchRaidHandler` / `HistoryItemEventHandler` | classes | `listener/handler/` | typed `AbstractMessageHandler<T>` beans |
| `StreamElementsSessionController` | class | `view/…:15` | authenticated `GET /streamelements/session` |
| `StreamElementsChannelController` | class | `view/…:14` | anonymous `GET /streamelements/channels/{channel}` (stub, authz commented out) |
| `StreamElementsSessionView` / `StreamElementsChannelView` | records | `view/` | JSON DTOs with static `of(...)` factories |

*Reference centrality not measured — no LSP available in this session.*

## CONVENTIONS
- **Serde is the serializer.** Primary `ObjectMapper` is `io.micronaut.serde.ObjectMapper`. Jackson is used **only** for `@JsonProperty` wire names (`_id`, kebab-case). Do not introduce a Jackson `ObjectMapper`.
- **Records for all data carriers** (events, DTOs, views) annotated `@Serdeable`; nested records are individually `@Serdeable`. Views expose static `of(...)` factories. Classes are only used for stateful entities, repositories, listeners, factories.
- **Null-safety is compile-enforced.** NullAway runs in JSpecify mode over `io.github.opendonationassistant`; every nullable field/param/return is annotated `@Nullable` (`org.jspecify.annotations`). No `package-info.java` / `@NullMarked` — the package is set via compiler arg. Boundaries null-guard with early `return`, not `Optional`.
- **Constructor injection only** (`@Inject` on constructor, `private final` fields); `var` used liberally; 2-space indent, Prettier-Java style.
- **Logging:** `private final ODALogger log = new ODALogger(this);` with `Map.of(...)` context (from oda-commons) — not slf4j directly.
- **Repository naming:** `*DataRepository` = thin `Map` wrapper returning `Optional`; `*Repository` (session) = aggregate returning `CompletableFuture`.
- **Rabbit listeners:** class-level `@RabbitListener(executor=...)`, `@Queue(QUEUE_NAME)` method, `public static final QUEUE_NAME` + `QUEUE` + `BINDING`; bindings wired centrally in `Application.rabbitConfiguration()`.
- **Controllers:** return `CompletableFuture<HttpResponse<...>>`; `@Secured` per endpoint; owner identity via `BaseController.getOwnerId(auth)`.

## ANTI-PATTERNS (THIS PROJECT)
- **No database/JPA** — state is a volatile embedded Infinispan map (`AdminFlag.VOLATILE`), lost on restart.
- **No direct slf4j logging** — use `ODALogger`.
- **Don't remove** `.mvn/jvm.config` `--add-exports`/`--add-opens`, or the `pom.xml` ErrorProne/NullAway `compilerArgs` (`-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, `-Xep:NullAway:ERROR`) — the build breaks.
- **No field injection** — constructor `@Inject`.
- **No Spring annotations** — this is Micronaut.

## UNIQUE STYLES
- `// prettier-ignore ON/OFF` markers preserve hand-formatted fluent builder chains (e.g. `Application.java:74`).
- Fully-qualified inline names are used deliberately to dodge clashes (e.g. `io.github.opendonationassistant.rabbit.Queue` in `EventsListener` vs Micronaut `@Queue`).

## COMMANDS
```bash
mvn test                                       # full suite (needs Docker: Infinispan test resource)
mvn test -Dtest=StreamElementsSessionRepositoryTest
mvn clean package                              # jar (default ${packaging}=jar)
mvn clean package -Dpackaging=native-image     # native binary at target/oda-streamelements-service
mvn verify sonar:sonar                         # CI (SonarCloud org opendonationassistant)
mvn mn:run                                     # run locally (requires JWKS_URI)
```
No Maven wrapper is committed (no `mvnw`) — use plain `mvn`.

## NOTES
- `Application.main` hardcodes `.defaultEnvironments("standalone")`, so `application-standalone.yml` **always** loads; it hardcodes Infinispan HotRod `10.43.81.28:11222` with `admin/password`. Override for local dev.
- `JWKS_URI` is the only required runtime env var (Keycloak JWKS for JWT bearer auth).
- `@MicronautTest` tests require a **Docker daemon** (`micronaut-test-resources-infinispan` spins up Infinispan via Testcontainers). There is **no RabbitMQ test resource** — contrary to older notes; Rabbit clients are mocked in unit tests.
- Listener executors are `fixed` `nThreads: 1` (`command-listener`, `config-listener`, `event-listener`); handlers block with `.join()`.
- **Dead/unwired code:** `SerdeableEntryMarshaller` (unreferenced), `EventsListener.repository` (injected, never used), `StreamElementsChannelController` repository check commented out (always returns 200 with a fabricated channel), `command-listener` executor declared but unused.
- **Duplicate test file:** `src/test/java/io/github/opendonationassistant/streamelements/listener/WidgetChangesEventListenerTest.java` is a tracked **0-byte** skeleton; the real 69-line test lives one level up in `io.github.opendonationassistant.listener`.
- No tests exist for `Application`, `EventsListener`, any handler, `WidgetFacade`, or the repositories' error paths.
- OpenAPI spec is generated to `target/classes/META-INF/swagger/`; the npm client is configured by `openapi-config.json`.
- AOT properties freeze env/cache at build time and disable remote JWKS fetch during the build.
