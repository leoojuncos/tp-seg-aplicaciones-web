-- Rol con el que se conecta el monolito: sin superusuario y solo con lectura y escritura de filas.
-- El login arma su consulta por concatenacion (paso 1 del escenario, ver AGENTS.md), y con el
-- superusuario sgm esa inyeccion llegaria tambien a funciones como pg_read_file o
-- pg_terminate_backend, que van mas alla de "entrar sin conocer la contrasena". sgm queda para
-- inicializar la base. Corre al final de db/init/ para alcanzar a todas las tablas: un script nuevo
-- que cree tablas va antes que este.
CREATE ROLE sgm_monolith LOGIN PASSWORD 'sgm_monolith' NOSUPERUSER NOCREATEDB NOCREATEROLE;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO sgm_monolith;
-- Las columnas GENERATED ... AS IDENTITY toman el valor de una secuencia.
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sgm_monolith;
