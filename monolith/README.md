# Monolith

Spring Boot monolith (`monolith`) for the SGM (Seguridad, Administracion, Tesoreria, VEP), per [TPS-9](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-9).

## Prerequisites

- **JDK 21** (the project targets Java 21; make sure IntelliJ has one configured — see below).
- **Docker**, to run Postgres and RabbitMQ locally with the repo's `docker-compose.yml` (see [Running locally](#running-locally)).
- You do **not** need Maven installed — the repo commits the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads the right Maven version on first run.

## Opening the project in IntelliJ

1. **File > Open...** and select the `monolith/` folder (or `monolith/pom.xml` directly — IntelliJ will offer to open it as a project).
2. IntelliJ detects it as a Maven project and imports the dependencies automatically. If it doesn't prompt, right-click `pom.xml` > **Maven > Reload project**.
3. **Set the project SDK to 21**: **File > Project Structure > Project** — set *SDK* and *Language level* to 21. If no JDK 21 is listed, click **Add SDK > Download JDK...** (or point it at one already installed on your machine) from that same dropdown.
4. **Use the Maven Wrapper** instead of a bundled Maven version: **Settings > Build Tools > Maven** — set *Maven home path* to **Use Maven wrapper**. This keeps everyone on the same Maven version declared in `.mvn/wrapper/maven-wrapper.properties`.
5. IntelliJ auto-generates a Spring Boot run configuration for `MonolithApplication` (visible in the class's gutter icon, or under **Run > Edit Configurations...**). That's what you'll use to start the app.

## Opening the project in other editors (VS Code, Zed, etc.)

Nothing in the project depends on IntelliJ: it's a plain Maven project, so any editor works. What changes is how the editor understands the Java code and how you start the app.

In every case:

- **Open the `monolith/` folder as the workspace root**, not the repo root. Java tooling looks for `pom.xml` at the root of the workspace.
- **Have a JDK 21 or newer installed** and visible to the editor (on the `PATH`, or via `JAVA_HOME`). Maven compiles for Java 21 regardless of which JDK runs it.
- **Starting the app from the terminal always works**, whatever the editor: `./mvnw spring-boot:run` (or `mvnw.cmd spring-boot:run` on Windows cmd/PowerShell). Your editor's integrated terminal is fine.

### VS Code

1. Install the **Extension Pack for Java** (`vscjava.vscode-java-pack`). Optionally also install the **Spring Boot Extension Pack** (`vmware.vscode-boot-dev-pack`), which adds a Spring Boot Dashboard with start/stop buttons for the app.
2. Open the `monolith/` folder. The Java extension detects `pom.xml` and imports the project; the first import takes a minute (progress shows in the status bar).
3. If you have several JDKs installed and VS Code picks the wrong one, set `java.jdt.ls.java.home` (the JDK the extension itself runs on) and/or `java.configuration.runtimes` (the JDK used to run the project) in your user settings.
4. Open `MonolithApplication.java` and click **Run** (or **Debug**) above the `main` method.

### Zed

1. Install the **Java** extension (**Extensions** panel, search "Java"). It downloads and runs the same Java language server VS Code uses, which imports the Maven project from `pom.xml`.
2. Open the `monolith/` folder.
3. Start the app from Zed's terminal with `./mvnw spring-boot:run`. To get a reusable command instead, add a task in `.zed/tasks.json` (not committed; it's personal config) and run it with **task: spawn**:

   ```json
   [
     {
       "label": "Run monolith",
       "command": "./mvnw spring-boot:run",
       "cwd": "$ZED_WORKTREE_ROOT"
     }
   ]
   ```

   On Windows, use `mvnw.cmd spring-boot:run` as the command.

### Any other editor

Any editor with a Java language server (Neovim, Helix, Emacs, Sublime Text...) normally uses **Eclipse JDT LS**, which imports the project from `pom.xml` on its own. If the editor has no Java support at all, you can still edit the code and use the terminal to build and run it: `./mvnw spring-boot:run` to start the app, `./mvnw test` to run the tests.

## Running locally

Start Postgres and RabbitMQ from the repo root, with the shared `docker-compose.yml`:

```bash
docker compose up -d postgres rabbitmq
```

Start only those two: a plain `docker compose up` also runs the monolith in a container, which takes port `8080`. See [Puesta en marcha](../README.md#puesta-en-marcha) in the root README for the whole system.

Then either:

- **From IntelliJ**: run the `MonolithApplication` run configuration (green play button next to `main`).
- **From another editor**: see [Opening the project in other editors](#opening-the-project-in-other-editors-vs-code-zed-etc).
- **From the terminal**: `./mvnw spring-boot:run` (or `mvnw.cmd spring-boot:run` on Windows cmd/PowerShell).

The app connects using these defaults (overridable via env vars — see `src/main/resources/application.yml`): DB at `localhost:5432/sgm` (user/password `sgm_monolith`, a non-superuser role created by `db/init/99-monolith-role.sql`; if your local database predates that script, recreate it with `docker compose down -v`), RabbitMQ at `localhost:5672` (user/password `sgm`), app on port `8080`.

On startup, the logs should show `DB OK` and `RabbitMQ OK`. Verify with:

```bash
curl http://localhost:8080/api/health
# {"status":"UP","db":"UP","rabbitmq":"UP"}
```

If either dependency is down, `/api/health` responds `503` and reports which one, without crashing the app.

## Messaging module

The `messaging` package is the monolith's internal messaging module, the only part of the system that talks to the RabbitMQ queue. Other modules hand it audit events through `AuditEventPublisher`: it stores each event in the `pending_events` table, which is the source of truth, and puts it on the `auditoria.events` queue for the Auditoría service.

The module has its own technical accounts (`technical_accounts` table) and its own sessions, separate from the SGM session cookie. `POST /api/messaging/auth/login` returns a bearer token, and every other endpoint under `/api/messaging/` requires `Authorization: Bearer <token>`. Since the module authenticates its own requests, any SGM session filter must leave `/api/messaging/**` out.

| Endpoint | Returns |
| --- | --- |
| `POST /api/messaging/auth/login` | A session token for a technical account. |
| `GET /api/messaging/accounts` | The technical accounts, without passwords. |
| `GET /api/messaging/queue` | Messages and consumers of the audit queue. |
| `GET /api/messaging/pending` | The audit events, newest first. |
| `GET /api/messaging/pending/{id}` | One audit event. |

Payloads, the error format and the fixed technical accounts are in [`docs/contracts.md`](../docs/contracts.md). The tables come from `db/init/02-messaging-schema.sql`, which Postgres runs when the compose volume is created; if your local database predates that script, recreate it with `docker compose down -v`.

The seed loads the technical accounts. With them in the database:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/messaging/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "auditoria_lector", "password": "Auditoria2026!"}' | jq -r .token)
curl -s http://localhost:8080/api/messaging/pending -H "Authorization: Bearer $TOKEN"
```

## Tesorería module

The `tesoreria` package holds the taxpayers' debts (`debts` table, one per CUIT) and their forgiveness. Forgiving a debt sets its status to `FORGIVEN` and keeps the original amount.

| Endpoint | Returns |
| --- | --- |
| `GET /api/tesoreria/debts` | The debts, ordered by CUIT. With `?cuit=` (11 digits, no dashes), only that CUIT's debt. |
| `GET /api/tesoreria/debts/{id}` | One debt. |
| `POST /api/tesoreria/debts/{id}/forgive` | The forgiven debt, or `409` if it was already forgiven. |

These endpoints require the SGM session cookie (`sgm_session`). The cookie and the error format are described in [`docs/contracts.md`](../docs/contracts.md).

## Running the tests

```bash
./mvnw test
```

The tests need Postgres and RabbitMQ reachable (same as above): `MonolithApplicationTests` exercises the real connectivity checks, and `MessagingApiTest` and `TesoreriaApiTest` run the messaging and debts APIs against the real database, so they also need the tables from `db/init/` (see [Messaging module](#messaging-module)).

## A note on timezones

If you ever see `FATAL: invalid value for parameter "TimeZone"` when connecting to Postgres, it's a known clash between some JVM locale setups (common on Argentina-based machines) and the Postgres Docker image's timezone database. It's already handled for every normal way of running the app — a static initializer in `MonolithApplication` pins the JVM to UTC (covers `main()`, so also IntelliJ's run configuration), and `pom.xml` additionally pins `-Duser.timezone=UTC` for `./mvnw test`, since `@SpringBootTest` doesn't go through `main()`. You shouldn't need to configure anything extra.
