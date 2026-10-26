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

-- El auditor va primero: el paso 1 de la cadena cae en la primera fila de users, y con sus permisos
-- (Auditoria e Ingresos Publicos) la unica pista de que existe Tesoreria es el selector de modulos
-- del listado de Auditoria (paso 2).
-- El resto son usuarios de relleno para que Administracion se vea realista. Las contrasenas
-- van en BCrypt, generadas con crypt('<contrasena>', gen_salt('bf', 10)) de pgcrypto.
INSERT INTO users (username, password_hash, role_id) VALUES
    ('auditor',  '$2a$10$uN3euk1WQJxd8WW/HYT9vu7Y9Nr82tstHETRdRaTzP/BBTbFX3rsi', (SELECT id FROM roles WHERE code = 'AUDITOR')),        -- LupaFina58
    ('soporte',  '$2a$10$Kyx79pvRLhak.qlDpPuyT.5IYus4b8AbjxZ8NPqEEblCRmJd7B.gK', (SELECT id FROM roles WHERE code = 'SOPORTE')),        -- MesaDeAyuda41
    ('tesorero', '$2a$10$hngEqM6EbVUk09SBhGM24uIHVEvc0jZwGR3dRyavVsMUGi4wBLLZK', (SELECT id FROM roles WHERE code = 'TESORERO')),       -- CajaFuerte73
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
    ('auditor',  'INGRESOS_PUBLICOS'),
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

-- Deudas de ejemplo, una por CUIT. Las seis primeras quedan PENDING: la primera es la de la demo, con
-- el mismo CUIT del ejemplo del contrato del evento, y todavia no tiene evento en Auditoria. Las ultimas
-- son condonaciones que ya ocurrieron: cada una tiene su DEBT_FORGIVEN en audit_events (mas abajo), asi
-- el VEP las muestra Condonada y la pantalla de Auditoria arranca con historial en vez de vacia. Se
-- mantiene el invariante del escenario: una deuda condonada siempre tiene su evento registrado.
INSERT INTO debts (cuit, amount, status) VALUES
    ('20123456789',  15000.00, 'PENDING'),
    ('27283334444',  48250.50, 'PENDING'),
    ('30712345671', 312400.00, 'PENDING'),
    ('20301112220',   7300.00, 'PENDING'),
    ('30709998885', 125990.75, 'PENDING'),
    ('23256667770',   2150.00, 'PENDING'),
    ('30711222093',  89000.00, 'FORGIVEN'),
    ('27352221118',  15600.00, 'FORGIVEN'),
    ('20284445556', 230750.00, 'FORGIVEN'),
    ('33714567899',  51200.00, 'FORGIVEN'),
    ('23399887771',   9900.00, 'FORGIVEN'),
    ('20267778883',  42300.00, 'FORGIVEN');

-- Historial de Auditoria: eventos de todos los modulos, la mayoria de Tesoreria, para que la pantalla
-- de Auditoria arranque con movimiento y se vea que cada operacion deja registro. Cada deuda de arriba
-- tiene su alta (DEBT_CREATED), anterior a su condonacion, y cada condonada su DEBT_FORGIVEN: ese es el
-- registro que la cadena de ataque busca evitar. Por eso el CUIT de la demo (20123456789) no aparece en
-- ningun evento, de ningun tipo, y el objetivo del ensayo es que nunca llegue a aparecer. La condonacion
-- es el unico tipo que publica el sistema (ver docs/contracts.md); los demas son solo historial, y
-- Auditoria los asigna a su modulo en AuditEventService. occurred_at es cuando ocurrio el hecho y
-- registered_at cuando Auditoria lo registro, unos minutos despues (la demora del procesamiento). El id
-- es un UUID cualquiera (gen_random_uuid, de pgcrypto): no hace falta que coincida con ninguna fila de
-- messaging.
INSERT INTO audit_events (id, type, cuit, occurred_at, registered_at) VALUES
    -- Tesoreria
    (gen_random_uuid(), 'DEBT_CREATED',              '30711222093', '2026-09-02 10:12:00-03', '2026-09-02 10:15:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '27352221118', '2026-09-04 11:30:00-03', '2026-09-04 11:34:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '20284445556', '2026-09-08 09:45:00-03', '2026-09-08 09:48:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '27283334444', '2026-09-10 12:05:00-03', '2026-09-10 12:09:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '33714567899', '2026-09-15 15:20:00-03', '2026-09-15 15:23:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '30712345671', '2026-09-18 10:40:00-03', '2026-09-18 10:43:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '23399887771', '2026-09-22 13:10:00-03', '2026-09-22 13:14:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '20301112220', '2026-09-25 09:25:00-03', '2026-09-25 09:28:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '20267778883', '2026-09-29 16:00:00-03', '2026-09-29 16:04:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '30709998885', '2026-10-01 11:15:00-03', '2026-10-01 11:18:00-03'),
    (gen_random_uuid(), 'PAYMENT_SLIP_ISSUED',       '27283334444', '2026-10-02 09:50:00-03', '2026-10-02 09:53:00-03'),
    (gen_random_uuid(), 'DEBT_FORGIVEN',             '30711222093', '2026-10-03 09:15:00-03', '2026-10-03 09:18:00-03'),
    (gen_random_uuid(), 'DEBT_FORGIVEN',             '27352221118', '2026-10-06 14:40:00-03', '2026-10-06 14:44:00-03'),
    (gen_random_uuid(), 'PAYMENT_SLIP_ISSUED',       '30712345671', '2026-10-07 14:15:00-03', '2026-10-07 14:19:00-03'),
    (gen_random_uuid(), 'DEBT_CREATED',              '23256667770', '2026-10-08 10:30:00-03', '2026-10-08 10:33:00-03'),
    (gen_random_uuid(), 'DEBT_FORGIVEN',             '20284445556', '2026-10-09 11:05:00-03', '2026-10-09 11:09:00-03'),
    (gen_random_uuid(), 'PAYMENT_SLIP_ISSUED',       '20301112220', '2026-10-13 11:40:00-03', '2026-10-13 11:43:00-03'),
    (gen_random_uuid(), 'DEBT_FORGIVEN',             '33714567899', '2026-10-14 16:20:00-03', '2026-10-14 16:23:00-03'),
    (gen_random_uuid(), 'PAYMENT_SLIP_ISSUED',       '30709998885', '2026-10-16 10:05:00-03', '2026-10-16 10:09:00-03'),
    (gen_random_uuid(), 'DEBT_FORGIVEN',             '23399887771', '2026-10-17 10:50:00-03', '2026-10-17 10:54:00-03'),
    (gen_random_uuid(), 'DEBT_FORGIVEN',             '20267778883', '2026-10-20 12:30:00-03', '2026-10-20 12:33:00-03'),
    (gen_random_uuid(), 'PAYMENT_SLIP_ISSUED',       '23256667770', '2026-10-21 15:30:00-03', '2026-10-21 15:33:00-03'),
    (gen_random_uuid(), 'PAYMENT_SLIP_ISSUED',       '27283334444', '2026-10-23 10:20:00-03', '2026-10-23 10:24:00-03'),
    -- Ingresos Publicos
    (gen_random_uuid(), 'TAXPAYER_REGISTERED',       '30715556662', '2026-09-05 10:00:00-03', '2026-09-05 10:03:00-03'),
    (gen_random_uuid(), 'TAX_RETURN_FILED',          '30712345671', '2026-09-12 17:45:00-03', '2026-09-12 17:49:00-03'),
    (gen_random_uuid(), 'TAX_ADDRESS_CHANGED',       '27283334444', '2026-09-19 12:30:00-03', '2026-09-19 12:33:00-03'),
    (gen_random_uuid(), 'TAX_RETURN_FILED',          '30709998885', '2026-10-10 18:20:00-03', '2026-10-10 18:23:00-03'),
    (gen_random_uuid(), 'TAXPAYER_REGISTERED',       '20389990004', '2026-10-15 09:35:00-03', '2026-10-15 09:39:00-03'),
    (gen_random_uuid(), 'TAX_RETURN_FILED',          '33714567899', '2026-10-22 16:50:00-03', '2026-10-22 16:53:00-03'),
    -- Contaduria
    (gen_random_uuid(), 'SUPPLIER_INVOICE_RECORDED', '30716660017', '2026-09-09 11:00:00-03', '2026-09-09 11:03:00-03'),
    (gen_random_uuid(), 'PAYMENT_ORDER_ISSUED',      '30716660017', '2026-09-23 10:30:00-03', '2026-09-23 10:34:00-03'),
    (gen_random_uuid(), 'SUPPLIER_INVOICE_RECORDED', '30708881239', '2026-10-05 15:10:00-03', '2026-10-05 15:13:00-03'),
    (gen_random_uuid(), 'PAYMENT_ORDER_ISSUED',      '30708881239', '2026-10-19 09:55:00-03', '2026-10-19 09:58:00-03'),
    (gen_random_uuid(), 'SUPPLIER_INVOICE_RECORDED', '33707772225', '2026-10-23 14:40:00-03', '2026-10-23 14:44:00-03'),
    -- Administracion (el CUIT es el CUIL del agente)
    (gen_random_uuid(), 'USER_CREATED',              '20321114446', '2026-09-01 09:10:00-03', '2026-09-01 09:13:00-03'),
    (gen_random_uuid(), 'PERMISSION_GRANTED',        '20321114446', '2026-09-01 09:14:00-03', '2026-09-01 09:17:00-03'),
    (gen_random_uuid(), 'USER_CREATED',              '27345556661', '2026-09-16 10:25:00-03', '2026-09-16 10:29:00-03'),
    (gen_random_uuid(), 'PERMISSION_GRANTED',        '27345556661', '2026-10-12 11:50:00-03', '2026-10-12 11:53:00-03'),
    -- Auditoria (CUIL del auditor)
    (gen_random_uuid(), 'EVENTS_EXPORTED',           '20298887775', '2026-09-30 17:30:00-03', '2026-09-30 17:33:00-03'),
    (gen_random_uuid(), 'EVENTS_EXPORTED',           '20298887775', '2026-10-24 12:15:00-03', '2026-10-24 12:18:00-03');
