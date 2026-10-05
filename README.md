# tp-seg-aplicaciones-web

Sistema de Gestión Municipal (SGM) mínimo y **deliberadamente vulnerable**, para el TP de Seguridad en Aplicaciones Web (UTN FRBA). Sirve para demostrar en vivo una cadena de cuatro vulnerabilidades del OWASP Top 10 y sus mitigaciones.

## Arquitectura

- **Monolito**: contiene Seguridad (login), Administración, Tesorería y el VEP.
- **Módulo de mensajería** (`messaging`): paquete interno del monolito y único punto de acceso a la cola. Guarda los eventos de auditoría pendientes y tiene cuentas técnicas y sesión propias, independientes de la sesión del SGM.
- **Microservicio de Auditoría**: servicio aparte que se comunica con el monolito a través de RabbitMQ.
- **RabbitMQ**: cola de mensajes, del lado del monolito. Tesorería publica los eventos a través del módulo de mensajería y el microservicio de Auditoría los consume.
- **PostgreSQL**: una única base compartida.
- Todo se levanta con un único `docker-compose`.

Los contratos entre las partes (el formato del evento, la API del módulo de mensajería y sus cuentas técnicas) están en [`docs/contracts.md`](docs/contracts.md).

## Stack y versiones

| Capa | Tecnología | Versión |
| --- | --- | --- |
| Backend | Java + Spring Boot (build con Maven) | Java 21 LTS · Spring Boot 3.5.x |
| Base de datos | PostgreSQL | 16 |
| Cola de mensajes | RabbitMQ (imagen `-management`) | 3.13 |
| Frontend | React + Vite (JavaScript, sin TypeScript) | React 18 |
| Runtime del frontend | Node.js | 22 LTS |

**Criterio de elección:** versiones estables y con amplia cobertura de material, por encima de lo más nuevo, dado que el equipo arranca con Spring.

## Decisiones

Las decisiones de arquitectura, stack y contratos están registradas y justificadas en el ticket **TPS-4** de Jira. Cualquier cambio se acuerda ahí y se refleja acá.

## Puesta en marcha

Hace falta Docker con Compose. Desde la raíz del repo:

```bash
docker compose up -d --build
```

Levanta Postgres, RabbitMQ, el monolito y Auditoría. Las apps arrancan cuando Postgres terminó de inicializar la base y RabbitMQ acepta conexiones. `--build` va siempre: sin él, Compose reusa las imágenes anteriores y no toma los cambios del código.

Los puertos se publican solo en `127.0.0.1`:

| Servicio | Dirección |
| --- | --- |
| Monolito | http://localhost:8080/api/health |
| Auditoría | http://localhost:8081/api/health |
| UI de RabbitMQ | http://localhost:15672 (usuario y contraseña `sgm`) |
| Postgres | `localhost:5432`, base `sgm` (usuario y contraseña `sgm`) |

En el primer arranque, Postgres corre los scripts de `db/init/`. `docker compose down -v` borra la base: el próximo `up` la vuelve a crear desde esos scripts.

Para correr una app desde el IDE, levantar solo Postgres y RabbitMQ con `docker compose up -d postgres rabbitmq`. El README de cada servicio lo explica.

Si siguen corriendo los contenedores `sgm-postgres` y `sgm-rabbit` que indicaban antes los README de los servicios, ocupan los mismos puertos y el `up` falla. Se borran con `docker rm -f sgm-postgres sgm-rabbit`.

### Datos de ejemplo

El seed (`db/init/90-seed.sql`) carga los datos con los que se hace la demo:

| Usuario | Contraseña | Rol | Permisos |
| --- | --- | --- | --- |
| `soporte` | `MesaDeAyuda41` | Soporte | Administración, Auditoría |
| `tesorero` | `CajaFuerte73` | Tesorero | Tesorería |
| `auditor` | `LupaFina58` | Auditor | Auditoría |
| `admin` | `LlaveMaestra92` | Administrador | Administración, Tesorería, Auditoría |
| `operador` | `VentanillaTres17` | Operador | ninguno |

Hay una deuda de ejemplo: CUIT `20123456789`, $15.000, pendiente. Las cuentas técnicas del módulo de mensajería están en [`docs/contracts.md`](docs/contracts.md).

Lo que cambia una demo, como una deuda condonada, queda guardado en la base. Para volver a estos datos: `docker compose down -v` y otra vez `up`.

## Review automática de PRs

Los PRs los revisa un reviewer automático: Claude Opus, con una segunda opinión de Gemini y MiniMax sobre los hallazgos que piden cambios. La review se pide con etiquetas.

| Etiqueta | La pone | Qué pasa |
| --- | --- | --- |
| `reviewable` | cualquiera del equipo | El PR entra en la cola de la noche, que arranca alrededor de las 03:00 (hora argentina). Si GitHub la demora hasta el horario laboral (9 a 17), queda para la noche siguiente. |
| `review-now` | cualquiera del equipo, siempre junto con `reviewable` | La review corre en el momento. El bot saca la etiqueta enseguida y comenta si arrancó o por qué no. Leer la advertencia de abajo antes de usarla. |
| `in-review` | el bot | Se está revisando. `reviewable` sigue puesto hasta el veredicto: si la review falla o se pushea mientras corre, el PR sigue en la cola. La cola de la noche no lanza otra review para un PR que ya se está revisando. |
| `approved` / `changes-requested` | el bot | Es el veredicto. El bot saca `reviewable`. |
| `review-failed` | el bot | La review falló dos veces seguidas y el PR salió de la cola: avisar a @leoojuncos. |

> [!WARNING]
> **`review-now` es SOLO para algo MUY importante que no puede esperar a la noche.** Cada review inmediata gasta en el momento el cupo de Claude de @leoojuncos, en pleno horario de trabajo. Hay un tope de 2 por día para todo el equipo (las que pide @leoojuncos no cuentan): pasado el tope, el bot saca la etiqueta y el PR queda para la noche. Si no está claro que sea urgente, no lo es: usar `reviewable`.

### Qué mira el reviewer

Además del código, el reviewer lee el ticket de Jira que figura en el título del PR (`TPS-…`) y, de la descripción, solo la sección `Aclaraciones`. Ahí van los desvíos respecto del ticket, con su motivo: algo que el ticket pide y no se hizo, o algo que se hizo y el ticket no pide. Un desvío explicado en `Aclaraciones` no se marca; uno sin explicar, sí. También va ahí el motivo de un cambio a la lista de vulnerabilidades. El resto de la descripción no se lee.

### Cómo leer el veredicto

Cada pasada deja un comentario con los hallazgos: los que piden cambios, numerados F1, F2…, y los informativos, I1, I2…. Cada uno trae su severidad, el `archivo:línea` y, cuando ayuda, el fragmento de código o una propuesta concreta.

- `BLOCK` y `WARN` piden cambios. `INFO` no bloquea.
- El veredicto vale para el commit revisado: un push después del `approved` saca la etiqueta.
- Si se pushea mientras está `in-review`, el veredicto sale igual pero no se aplica, y el PR vuelve a la cola.

### Cómo iterar

1. Corregir lo que corresponda, con amend o fixup como indica `AGENTS.md`.
2. Si un hallazgo no corresponde, responder en el PR con su número y la razón. Si la razón se sostiene, la pasada siguiente lo marca `justified` y deja de bloquear.
3. Volver a poner `reviewable`. La pasada siguiente corre a la noche, o en el momento con `review-now` (misma advertencia, mismo tope), y no repite lo ya resuelto o justificado.

Las vulnerabilidades buscadas del escenario están listadas en `AGENTS.md`: el reviewer no las marca como defecto. Sí marca un cambio que altera una sin ser su mitigación, una mitigación que deja alguna ruta sin cubrir y cualquier cambio a la lista. Una vulnerabilidad que no está en la lista es un defecto y pide cambios, aunque parezca intencional.

Solo se revisan PRs de ramas de este repo, no de forks.
