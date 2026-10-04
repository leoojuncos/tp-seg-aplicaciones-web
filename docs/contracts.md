# Contratos del SGM

Un apartado por contrato, con un ejemplo. Cada uno lo escribe el ticket que lo define y el resto del sistema se ajusta a lo que dice acá.

## Evento de auditoría

Lo publica el módulo de mensajería del monolito (`messaging`) cuando otro módulo le entrega un evento, y lo consume el microservicio de Auditoría. La tabla `pending_events` del monolito es la fuente de verdad del estado del evento: RabbitMQ solo lo transporta.

- **Cola:** `auditoria.events`, en el exchange por defecto (la routing key es el nombre de la cola).
- **Declaración:** el monolito y Auditoría declaran la cola con las mismas propiedades: durable, no exclusiva, sin autodelete y sin argumentos. Si difieren, RabbitMQ rechaza la segunda declaración.
- **Mensaje:** el cuerpo es el JSON del evento, en UTF-8, con `content-type: application/json`. Los headers del mensaje no forman parte del contrato.

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `id` | string (UUID) | Identificador del evento. Lo genera el módulo de mensajería. |
| `type` | string | Tipo de evento. Valor definido: `DEBT_FORGIVEN` (condonación de una deuda). |
| `cuit` | string | CUIT del contribuyente: 11 dígitos, sin guiones. |
| `timestamp` | string (ISO-8601, UTC) | Momento en que ocurrió el hecho auditado, con precisión de microsegundos. |
| `status` | string | `PENDING` al publicarse. `CANCELLED` si se dio de baja antes de que Auditoría lo registrara. |

Ejemplo:

```json
{
  "id": "3f0c9a52-6d1e-4c8b-9a37-2b5f1e7d4c10",
  "type": "DEBT_FORGIVEN",
  "cuit": "20123456789",
  "timestamp": "2026-10-04T21:15:30.123456Z",
  "status": "PENDING"
}
```

El mensaje siempre sale con `status: PENDING`; el estado vigente se consulta en el módulo, no en el mensaje. Antes de registrar un evento, Auditoría pide `GET /api/messaging/pending/{id}` con la cuenta de solo lectura y lo registra solo si la respuesta es `200` con `status: PENDING`. Si responde `404` o `status: CANCELLED`, lo descarta.

## API del módulo de mensajería

Endpoints del monolito bajo `/api/messaging/`. Dentro del docker-compose se llega por `MONOLITH_URL` (`http://monolith:8080`); desde el host, por `http://localhost:8080`; desde el front, por el mismo camino a través de su proxy de `/api`.

### Sesión del módulo

La sesión es propia del módulo e independiente de la cookie de sesión del SGM.

- `POST /api/messaging/auth/login` con `{ "username": "...", "password": "..." }` autentica contra las cuentas técnicas y devuelve un token:

  ```json
  { "token": "q8V3...", "username": "auditoria_lector", "role": "READ", "expiresAt": "2026-10-04T21:45:30Z" }
  ```

- El resto de los endpoints pide el header `Authorization: Bearer <token>`.
- El token vence a los 30 minutos (configurable con la variable `MESSAGING_SESSION_TTL`, por ejemplo `10m`).
- Las sesiones viven en la memoria del monolito: si se reinicia, hay que volver a autenticarse.

### Endpoints

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `POST /api/messaging/auth/login` | sin sesión | La sesión, como en el ejemplo de arriba. |
| `GET /api/messaging/accounts` | `READ` o `WRITE` | Las cuentas técnicas, sin contraseñas: `[{ "username", "role", "serviceId" }]`. |
| `GET /api/messaging/queue` | `READ` o `WRITE` | El estado de la cola: `{ "queue": "auditoria.events", "messages": 0, "consumers": 1 }`. |
| `GET /api/messaging/pending` | `READ` o `WRITE` | Los eventos con el formato del mensaje, del `timestamp` más reciente al más antiguo. |
| `GET /api/messaging/pending/{id}` | `READ` o `WRITE` | Un evento, con el formato del mensaje. |

Los errores responden `{ "error": "<código>", "message": "<detalle>" }`:

| HTTP | `error` | Cuándo |
| --- | --- | --- |
| 400 | `validation` | Falta un campo o tiene un formato inválido. |
| 401 | `unauthorized` | Las credenciales no son válidas, o el token falta, no existe o venció. |
| 404 | `not_found` | El evento no existe. |
| 503 | `rabbitmq_unavailable` | No se pudo consultar RabbitMQ. |

Ese formato es el de los endpoints del módulo. Lo que falla antes de llegar a uno de ellos (una ruta que no existe, un método o un content-type que no acepta) y los errores internos inesperados, como la base caída, responden con el formato por defecto de Spring Boot.

La publicación no es un endpoint. Los módulos del monolito le entregan el evento al módulo de mensajería en el mismo proceso (`AuditEventPublisher`), que lo guarda como pendiente y lo encola. Ningún otro módulo accede a la cola.

## Cuentas técnicas del módulo de mensajería

Son valores fijos de la demo. El seed carga las filas en `technical_accounts` con estos hashes BCrypt, y las variables del docker-compose repiten los datos de la cuenta de solo lectura.

| Cuenta | Rol | Contraseña | Identificador del servicio | Hash BCrypt para el seed |
| --- | --- | --- | --- | --- |
| `auditoria_lector` | `READ` | `Auditoria2026!` | `AUD-FISCAL` | `$2a$10$ZK0qP7R9p/wICcTOCAKt9.ktRYVyC/VY.ez2lGvjop9HPT/Lb5LbG` |
| `auditoria_operador` | `WRITE` | no se publica | `AUD-FISCAL` | `$2a$10$m29ldag9XFQ7swlxK5pQ4OTgyqhPwiemlnaly8z1bnpJ5C2e1mgK6` |

La contraseña de la cuenta de escritura es aleatoria y no se publica: el Diagnóstico de integraciones expone la de solo lectura, y la sesión de escritura tiene que salir solo de la recuperación de la cuenta. El seed necesita únicamente el hash; para probar operaciones de escritura en local, se carga una cuenta propia.

Variables del docker-compose, disponibles en el monolito y en Auditoría:

| Variable | Valor |
| --- | --- |
| `MONOLITH_URL` | `http://monolith:8080` |
| `MESSAGING_READ_USER` | `auditoria_lector` |
| `MESSAGING_READ_PASSWORD` | `Auditoria2026!` |
