# Auditoría

Spring Boot microservice (`auditoria`) for the SGM. It consumes the audit events that the monolith's messaging module publishes to RabbitMQ and keeps an append-only record of the ones it registers. The event format and the messaging API it calls are defined in [`docs/contracts.md`](../docs/contracts.md).

## Prerequisites

- **JDK 21** (the project targets Java 21; make sure your IDE has one configured).
- **Docker**, to run Postgres and RabbitMQ locally with the repo's `docker-compose.yml` (see [Running locally](#running-locally)).
- You do **not** need Maven installed — the repo commits the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads the right Maven version on first run.

## Opening the project

It's a plain Maven project, so any editor works. **Open the `auditoria/` folder as the workspace root** (not the repo root): Java tooling looks for `pom.xml` at the root of the workspace. Have a JDK 21 visible to the editor (on the `PATH` or via `JAVA_HOME`).

- **IntelliJ**: *File > Open...* and select `auditoria/` (or `auditoria/pom.xml`). Set the project SDK to 21 in *File > Project Structure > Project*. IntelliJ auto-generates a run configuration for `AuditoriaApplication`.
- **VS Code**: install the *Extension Pack for Java* and open the `auditoria/` folder.
- **Any editor**: `./mvnw spring-boot:run` from the integrated terminal always works.

## Running locally

Start Postgres and RabbitMQ from the repo root, with the shared `docker-compose.yml`. The ports are bound to `127.0.0.1` so they're not exposed to the rest of the network:

```bash
docker compose up -d postgres rabbitmq
```

Start only those two: a plain `docker compose up` also runs auditoria in a container, which takes port `8081`. See [Puesta en marcha](../README.md#puesta-en-marcha) in the root README for the whole system.

> The monolith and auditoria share the same Postgres and RabbitMQ, so if you already have them up for the monolith you don't need to start them again.

Then start the app:

```bash
./mvnw spring-boot:run      # or mvnw.cmd spring-boot:run on Windows cmd/PowerShell
```

The app connects using these defaults (overridable via env vars — see `src/main/resources/application.yml`): DB at `localhost:5432/sgm` (user/password `sgm`), RabbitMQ at `localhost:5672` (user/password `sgm`), app on port **`8081`** (the monolith uses `8080`).

On startup, the logs should show `DB OK` and `RabbitMQ OK`, and the queue `auditoria.events` gets declared on the broker. Verify the health endpoint with:

```bash
curl http://localhost:8081/api/health
# {"status":"UP","db":"UP","rabbitmq":"UP"}
```

If either dependency is down, `/api/health` responds `503` and reports which one, without crashing the app.

## Consuming audit events

The micro declares a durable queue (`auditoria.events` by default, overridable via `AUDITORIA_QUEUE`) — the same declaration the monolith's messaging module uses, which is the only producer — and listens on it. For each message:

1. It waits a deliberate delay (`app.auditoria.delay`, env `AUDITORIA_DELAY`, default 3 minutes) before processing. While it waits, the event stays pending.
2. It then confirms the event's current state against the messaging module (`GET /api/messaging/pending/{id}`, with the read-only technical account), since the queue only transports the event — the module's store is the source of truth.
3. If the module confirms the event is still `PENDING`, it records it in the `audit_events` table — using the fields the module returns, not the message's, since the module is the source of truth — append-only, with its own `registered_at`. It discards the event only when the module says so: a `CANCELLED` status or the contract's `404 not_found`. Any other, non-conclusive response is retried rather than taken as a discard. It never writes back to the module.

Messages are consumed one at a time (prefetch 1), so when several are queued their delays add up — the consumer is deliberately slow. Re-delivered messages are ignored: a record already exists for that event id. A malformed message is logged and discarded, since retrying it wouldn't help. But if the messaging module can't be reached to confirm the event, the message is left on the queue and retried later rather than dropped, so a transient outage doesn't silently lose the audit record; the deliberate delay before each attempt keeps the retries from busy-looping.

The messaging API access is configured under `app.messaging` (`url`, `read-user`, `read-password`), defaulting to the contract's values so the micro runs from the IDE; inside docker-compose they come from `MONOLITH_URL`, `MESSAGING_READ_USER` and `MESSAGING_READ_PASSWORD`. The `audit_events` table is created by `db/init`, not by the app.

## Running the tests

```bash
./mvnw test
```

The processing logic, the messaging client and the listener have unit tests that don't need any infrastructure. The context-load test (`AuditoriaApplicationTests`) does need Postgres and RabbitMQ reachable (same as above), since starting the context brings up the real connectivity checks and the queue listener — they're not mocked.

## A note on timezones

The JVM is pinned to UTC (static initializer in `AuditoriaApplication`, plus `-Duser.timezone=UTC` in `pom.xml` for the tests). This avoids a known clash between some JVM locale setups (common on Argentina-based machines) and the Postgres Docker image's timezone database (`FATAL: invalid value for parameter "TimeZone"`). Business dates that get added later must use an explicit zone, since under UTC `LocalDate.now()` already rolls to the next day late in the evening (ART).
