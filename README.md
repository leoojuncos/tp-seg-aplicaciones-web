# tp-seg-aplicaciones-web

Sistema de Gestión Municipal (SGM) mínimo y **deliberadamente vulnerable**, para el TP de Seguridad en Aplicaciones Web (UTN FRBA). Sirve para demostrar en vivo una cadena de cuatro vulnerabilidades del OWASP Top 10 y sus mitigaciones.

## Arquitectura

- **Monolito**: contiene Seguridad (login), Administración, Tesorería y el VEP.
- **Microservicio de Auditoría**: servicio aparte que se comunica con el monolito a través de RabbitMQ.
- **RabbitMQ**: cola de mensajes, del lado del monolito. Tesorería publica los eventos y el microservicio de Auditoría los consume.
- **PostgreSQL**: una única base compartida.
- Todo se levanta con un único `docker-compose`.

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

Pendiente: se documenta cuando esté el `docker-compose` (ticket de Infraestructura).

## Review automática de PRs

Los PRs los revisa un reviewer automático: Claude Opus, con una segunda opinión de Gemini y MiniMax sobre los hallazgos que bloquean. La review se pide con etiquetas.

| Etiqueta | La pone | Qué pasa |
| --- | --- | --- |
| `reviewable` | cualquiera del equipo | El PR entra en la cola de la noche, que arranca alrededor de las 03:00 (hora argentina). Si GitHub la demora hasta el horario laboral (9 a 17), queda para la noche siguiente. |
| `review-now` | cualquiera del equipo, siempre junto con `reviewable` | La review corre en el momento. Leer la advertencia de abajo antes de usarla. |
| `in-review` | el bot | Se está revisando. |
| `approved` / `changes-requested` | el bot | Es el veredicto. El bot saca `reviewable`. |
| `review-failed` | el bot | La review falló dos veces seguidas: avisar a @leoojuncos. |

> [!WARNING]
> **`review-now` es SOLO para algo MUY importante que no puede esperar a la noche.** Cada review inmediata gasta en el momento el cupo de Claude de @leoojuncos, en pleno horario de trabajo. Hay un tope de 2 por día para todo el equipo: pasado el tope, el bot saca la etiqueta y el PR queda para la noche. Si no está claro que sea urgente, no lo es: usar `reviewable`.

### Qué mira el reviewer

Además del código, el reviewer lee el ticket de Jira que figura en el título del PR (`TPS-…`) y, de la descripción, solo la sección `Aclaraciones`. Ahí van los desvíos respecto del ticket, con su motivo: algo que el ticket pide y no se hizo, o algo que se hizo y el ticket no pide. Un desvío explicado en `Aclaraciones` no se marca; uno sin explicar, sí. El resto de la descripción no se lee.

### Cómo leer el veredicto

Cada pasada deja un comentario con los hallazgos numerados (F1, F2…), su severidad y el `archivo:línea`.

- `BLOCK` y `WARN` piden cambios. `INFO` no bloquea.
- El veredicto vale para el commit revisado: un push después del `approved` saca la etiqueta.
- Si se pushea mientras está `in-review`, el veredicto sale igual pero no se aplica, y el PR vuelve a la cola.

### Cómo iterar

1. Corregir lo que corresponda, con amend o fixup como indica `AGENTS.md`.
2. Si un hallazgo no corresponde, responder en el PR con su número y la razón. Si la razón se sostiene, la pasada siguiente lo marca `justified` y deja de bloquear.
3. Volver a poner `reviewable`. La pasada siguiente corre a la noche y no repite lo ya resuelto o justificado.

Las vulnerabilidades buscadas del escenario están listadas en `AGENTS.md`: el reviewer no las marca como defecto, pero avisa si un cambio las rompe o si aparece otra que no está en la lista.

Solo se revisan PRs de ramas de este repo, no de forks.
