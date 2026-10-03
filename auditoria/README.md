# Auditoría

Spring Boot microservice (`auditoria`) for the SGM. It consumes the audit events that Tesorería publishes to RabbitMQ; see [TPS-10](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-10).

This is the initial skeleton: the app builds, starts, exposes a health endpoint and stays listening on the queue, but it does **not** process events yet. The real consumer (deliberate delay, pending event, persistence) lands in [TPS-22](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-22).

## Prerequisites

- **JDK 21** (the project targets Java 21; make sure your IDE has one configured).
- **Docker**, to run Postgres and RabbitMQ locally. The shared `docker-compose.yml` for the whole system ([TPS-11](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-11)) doesn't exist yet, so for now start them standalone (see [Running locally](#running-locally)).
- You do **not** need Maven installed — the repo commits the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads the right Maven version on first run.

## Opening the project

It's a plain Maven project, so any editor works. **Open the `auditoria/` folder as the workspace root** (not the repo root): Java tooling looks for `pom.xml` at the root of the workspace. Have a JDK 21 visible to the editor (on the `PATH` or via `JAVA_HOME`).

- **IntelliJ**: *File > Open...* and select `auditoria/` (or `auditoria/pom.xml`). Set the project SDK to 21 in *File > Project Structure > Project*. IntelliJ auto-generates a run configuration for `AuditoriaApplication`.
- **VS Code**: install the *Extension Pack for Java* and open the `auditoria/` folder.
- **Any editor**: `./mvnw spring-boot:run` from the integrated terminal always works.

## Running locally

Postgres and RabbitMQ aren't containerized together yet (that's TPS-11), so start them standalone first. The ports are bound to `127.0.0.1` so they're not exposed to the rest of the network:

```bash
docker run -d --name sgm-postgres -e POSTGRES_USER=sgm -e POSTGRES_PASSWORD=sgm -e POSTGRES_DB=sgm -p 127.0.0.1:5432:5432 postgres:16
docker run -d --name sgm-rabbit -e RABBITMQ_DEFAULT_USER=sgm -e RABBITMQ_DEFAULT_PASS=sgm -p 127.0.0.1:5672:5672 -p 127.0.0.1:15672:15672 rabbitmq:3.13-management
```

> The monolito and auditoria share the same Postgres and RabbitMQ, so if you already have them up for the monolito you don't need to start them again.

Then start the app:

```bash
./mvnw spring-boot:run      # or mvnw.cmd spring-boot:run on Windows cmd/PowerShell
```

The app connects using these defaults (overridable via env vars — see `src/main/resources/application.yml`): DB at `localhost:5432/sgm` (user/password `sgm`), RabbitMQ at `localhost:5672` (user/password `sgm`), app on port **`8081`** (the monolito uses `8080`).

On startup, the logs should show `DB OK` and `RabbitMQ OK`, and the queue `auditoria.eventos` gets declared on the broker. Verify the health endpoint with:

```bash
curl http://localhost:8081/api/health
# {"status":"UP","db":"UP","rabbitmq":"UP"}
```

If either dependency is down, `/api/health` responds `503` and reports which one, without crashing the app.

## The audit queue

The micro declares a durable queue (`auditoria.eventos` by default, overridable via `AUDITORIA_QUEUE`) and keeps a listener attached to it. For now the listener only logs that a message arrived — it doesn't deserialize or process it. The definitive contract (queue/exchange/routing key and the event payload) is settled in [TPS-18](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-18) (producer) and [TPS-22](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-22) (consumer).

## Running the tests

```bash
./mvnw test
```

The context-load test (`AuditoriaApplicationTests`) needs Postgres and RabbitMQ reachable (same as above), since starting the context brings up the real connectivity checks and the queue listener — they're not mocked.

## A note on timezones

The JVM is pinned to UTC (static initializer in `AuditoriaApplication`, plus `-Duser.timezone=UTC` in `pom.xml` for the tests). This avoids a known clash between some JVM locale setups (common on Argentina-based machines) and the Postgres Docker image's timezone database (`FATAL: invalid value for parameter "TimeZone"`). Business dates that get added later must use an explicit zone, since under UTC `LocalDate.now()` already rolls to the next day late in the evening (ART).
