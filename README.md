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
