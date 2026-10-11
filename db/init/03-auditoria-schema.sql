-- Esquema del microservicio de Auditoria: el registro de los eventos que ya proceso.
-- Postgres corre los scripts de db/init/ en el primer arranque del docker-compose y el CI los
-- aplica antes de los tests: las apps no crean tablas. La entidad AuditEvent mapea esta tabla.
-- Es un registro append-only: Auditoria solo inserta una fila por evento registrado. El rol del
-- monolito no tiene permisos sobre esta tabla: se los quita 99-monolith-role.sql.

CREATE TABLE audit_events (
    id            UUID        PRIMARY KEY,
    type          VARCHAR(50) NOT NULL,
    cuit          VARCHAR(11) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL
);
