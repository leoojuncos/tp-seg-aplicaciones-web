-- Datos de ejemplo de la demo. SQL directo, sin Flyway. Corre despues de 01 y 02 por el orden
-- alfabetico de db/init/. Sin ids explicitos: los genera la base, asi las apps pueden seguir
-- insertando por JPA.

INSERT INTO roles (code) VALUES
    ('SOPORTE'),
    ('TESORERO'),
    ('AUDITOR'),
    ('ADMINISTRADOR'),
    ('OPERADOR'),
    ('INSPECTOR');

-- El usuario de soporte va primero: el paso 1 de la cadena cae en la primera fila de users.
-- El resto son usuarios de relleno para que Administracion se vea realista. Las contrasenas
-- van en BCrypt, generadas con crypt('<contrasena>', gen_salt('bf', 10)) de pgcrypto.
INSERT INTO users (username, password_hash, role_id) VALUES
    ('soporte',  '$2a$10$Kyx79pvRLhak.qlDpPuyT.5IYus4b8AbjxZ8NPqEEblCRmJd7B.gK', (SELECT id FROM roles WHERE code = 'SOPORTE')),        -- MesaDeAyuda41
    ('tesorero', '$2a$10$hngEqM6EbVUk09SBhGM24uIHVEvc0jZwGR3dRyavVsMUGi4wBLLZK', (SELECT id FROM roles WHERE code = 'TESORERO')),       -- CajaFuerte73
    ('auditor',  '$2a$10$uN3euk1WQJxd8WW/HYT9vu7Y9Nr82tstHETRdRaTzP/BBTbFX3rsi', (SELECT id FROM roles WHERE code = 'AUDITOR')),        -- LupaFina58
    ('admin',    '$2a$10$foPV7tc44bQdhGnCPlUiueiMQfAdhPs7ZgGJ3rtccH7ftY4AiX/pW', (SELECT id FROM roles WHERE code = 'ADMINISTRADOR')),  -- LlaveMaestra92
    ('operador', '$2a$10$9pazoo5kDT70rwEwqnzVMOPhAQCb8Jd833/i1HtgW6L3gIg7JSiEy', (SELECT id FROM roles WHERE code = 'OPERADOR'));       -- VentanillaTres17

-- INGRESOS_PUBLICOS y CONTADURIA son de modulos decorativos: aparecen en el home segun el permiso,
-- como los demas, pero sus pantallas solo dicen "Seccion fuera de la demo". No tienen API.
INSERT INTO permissions (code) VALUES
    ('ADMINISTRACION'),
    ('TESORERIA'),
    ('AUDITORIA'),
    ('INGRESOS_PUBLICOS'),
    ('CONTADURIA');

INSERT INTO user_permissions (user_id, permission_id)
SELECT u.id, p.id
FROM (VALUES
    ('soporte',  'ADMINISTRACION'),
    ('soporte',  'AUDITORIA'),
    ('tesorero', 'TESORERIA'),
    ('tesorero', 'INGRESOS_PUBLICOS'),
    ('tesorero', 'CONTADURIA'),
    ('auditor',  'AUDITORIA'),
    ('admin',    'ADMINISTRACION'),
    ('admin',    'TESORERIA'),
    ('admin',    'AUDITORIA'),
    ('admin',    'INGRESOS_PUBLICOS'),
    ('admin',    'CONTADURIA')
) AS assignment (username, code)
JOIN users u ON u.username = assignment.username
JOIN permissions p ON p.code = assignment.code;

-- Cuentas tecnicas del modulo messaging: valores fijos de docs/contracts.md (hashes BCrypt).
-- El docker-compose repite los datos de la cuenta de lectura.
INSERT INTO technical_accounts (username, password_hash, role, service_id) VALUES
    ('auditoria_lector',   '$2a$10$ZK0qP7R9p/wICcTOCAKt9.ktRYVyC/VY.ez2lGvjop9HPT/Lb5LbG', 'READ',  'AUD-FISCAL'),
    ('auditoria_operador', '$2a$10$m29ldag9XFQ7swlxK5pQ4OTgyqhPwiemlnaly8z1bnpJ5C2e1mgK6', 'WRITE', 'AUD-FISCAL');

-- Deuda de ejemplo: el mismo CUIT del ejemplo del contrato del evento.
INSERT INTO debts (cuit, amount, status) VALUES
    ('20123456789', 15000.00, 'PENDING');
