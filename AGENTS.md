# AGENTS.md

Micronaut 5.0.2 service (Java 25, Maven) in the Open Donation Assistant platform. Emulates the StreamElements API and pushes tip/follow events to streamer widgets over RabbitMQ. **This is Micronaut, not Spring Boot.**

## Build & test

- `mvn -o test` — full suite offline (deps cached). 7 tests, BUILD SUCCESS.
- Single test: `mvn -o test -Dtest=StreamElementsSessionRepositoryTest`
- CI builds a GraalVM native image: `mvn clean package -Dpackaging=native-image`. The Dockerfile runs the resulting `target/oda-streamelements-service` binary.
- CI also runs `mvn verify sonar:sonar` (SonarCloud, org `opendonationassistant`).

## Compile-time enforcement (build fails if violated)

- NullAway + ErrorProne run as annotation processors with `-Xep:NullAway:ERROR` in JSpecify mode for package `io.github.opendonationassistant`. Every nullable field/param/return must be annotated `@Nullable` (from `org.jspecify.annotations`). Missing annotations = compile error.
- `.mvn/jvm.config` adds the `--add-exports/--add-opens` flags ErrorProne needs; don't remove them.

## Testing quirks

- `StreamElementsSessionControllerTest` uses JUnit 4 (`org.junit.Test`) and is **silently skipped** — no vintage engine is on the classpath. New tests must use JUnit 5 (`org.junit.jupiter.api.Test`).
- `@MicronautTest` tests auto-start test resources (embedded RabbitMQ + Infinispan) via `micronaut.test.resources.enabled=true`; no external services needed.
- Expected noise: RabbitMQ logs `NOT_FOUND - no exchange 'rpc'` during `@MicronautTest` shutdown. Harmless.
- Tests use Instancio (`@Given`) for random data and Mockito for mocks.

## Architecture

- **No database.** Session state lives in an embedded Infinispan cache named `streamelements` (exposed as `Map<String, StreamElementsData>`), created with `AdminFlag.VOLATILE` in `StreamElementsStateCacheConfiguration` — data is lost on restart.
- `Application.java` is a `@Factory` wiring: RabbitMQ exchange bindings, a remote HotRod `RemoteCacheManager`, the embedded cache manager, and a `@Named("commands")` RabbitClient.
- RabbitMQ listeners:
  - `EventsListener` — queue `streamelements.events`, bound to `history` (HistoryItemEvent) and `twitch` (TwitchChannelFollowEvent) exchanges. Handlers extend `AbstractMessageHandler<T>` from oda-commons.
  - `WidgetChangesEventListener` — queue `streamelements.config`, bound to `changes.widgets`.
- Outbound to UI: `WidgetFacade` publishes to RabbitMQ topic exchange `amq.topic` with binding `{recipientId}.streamelements`.
- Widget config lookup: `WidgetConfigClient` RPC on `rpc` exchange, queue `widget.config-request`, reply-to `amq.rabbitmq.reply-to`.
- Controllers return `CompletableFuture<HttpResponse<...>>` and use `@Secured` (`IS_ANONYMOUS` for `/streamelements/channels/{channel}`, `IS_AUTHENTICATED` for `/streamelements/session`). JWT auth via Keycloak JWKS (`JWKS_URI` env var).

## Conventions

- Records + `@Serdeable` for DTOs/views; constructor injection (`@Inject` on constructor); `var`; 2-space indent.
- Structured JSON logging via `ODALogger` (from oda-commons), not slf4j directly.
- `@OpenAPIDefinition` on `Application`; swagger YAML lands in `target/classes/META-INF/swagger/`. `openapi-config.json` configures the generated npm client.

## Dependencies & env

- `oda-rabbit-conf` / `oda-commons` (`io.github.opendonationassistant`) come from GitHub Packages Maven repo (`https://maven.pkg.github.com/opendonationassistant/oda-libraries`); CI injects credentials via `s4u/maven-settings-action`. Locally they resolve from `~/.m2`.
- `Application.main` sets `.defaultEnvironments("standalone")`, so `application-standalone.yml` always loads. It hardcodes an Infinispan HotRod host `10.43.81.28` (k8s cluster) with `admin/password` — override via env/props for local dev.

## CI / release

- Push to `main` triggers the reusable `OpenDonationAssistant/oda-libraries/.github/workflows/release_service.yml`. It runs Sonar, builds the native image, tags with `github.RUN_NUMBER`, pushes `ghcr.io/opendonationassistant/oda-streamelements-service`, publishes the OpenAPI spec to the docs repo, and publishes the npm client.