package ar.edu.utn.frba.tps.monolith.auth.service;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.mapper.AuthMapper;
import ar.edu.utn.frba.tps.monolith.auth.model.User;
import ar.edu.utn.frba.tps.monolith.auth.repository.UserRepository;
import ar.edu.utn.frba.tps.monolith.exception.UnauthorizedException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

/**
 * Login del SGM: el paso 1 de la cadena de vulnerabilidades (Injection, ver AGENTS.md). La
 * verificacion de credenciales arma la consulta por concatenacion de string, a proposito, y es
 * el unico punto del sistema con ese patron. Todo lo que sigue (cargar el usuario completo con
 * su rol y permisos) es JPA parametrizado: la vulnerabilidad queda acotada a esta sola consulta.
 */
@Service
public class AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final AuthMapper mapper;

    public AuthService(JdbcTemplate jdbcTemplate, UserRepository userRepository, AuthMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public SessionResponse login(String username, String password) {
        // Una sola sentencia: con una consulta armada por concatenacion de string, Postgres
        // acepta varias separadas por ";" en el mismo mensaje, y la inyeccion dejaria de ser solo
        // "entrar sin conocer la contrasena" (ver AGENTS.md) para pasar a escribir en la base
        // (un COMMIT apilado cierra la transaccion de solo lectura de abajo y lo que sigue corre
        // fuera de ella). La plantilla de la consulta no tiene ningun ";", asi que rechazarlo en
        // los datos del formulario garantiza una sola sentencia; el bypass clasico no lo usa.
        if (username.indexOf(';') >= 0 || password.indexOf(';') >= 0) {
            throw new UnauthorizedException("Usuario o clave incorrectos");
        }

        // SET TRANSACTION READ ONLY explicito: dentro de esa unica sentencia, que tampoco se pueda
        // escribir (por ejemplo, llamando a una funcion que modifique datos). El readOnly de
        // @Transactional por si solo no alcanza (pgjdbc no lo traduce de forma confiable a un
        // READ ONLY real del lado del servidor): este SET lo fuerza explicitamente, para la
        // transaccion actual, antes de correr la consulta vulnerable.
        jdbcTemplate.execute("SET TRANSACTION READ ONLY");
        // Y la transaccion termina siempre en rollback (el login no escribe nada): un
        // set_config(..., false) inyectado cambia la configuracion de la sesion de Postgres, que
        // sobrevive al commit y quedaria pegada a esa conexion del pool; con el rollback se deshace.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ORDER BY id ASC LIMIT 1: una inyeccion booleana en username (que comenta el resto de la
        // consulta, incluida la comparacion de password) siempre devuelve la primera fila real de
        // users, en vez de una lista o un error.
        String sql = "SELECT id FROM users WHERE username = '" + username + "' "
                + "AND password_hash = crypt('" + password + "', password_hash) "
                + "ORDER BY id ASC LIMIT 1";
        List<Long> ids;
        try {
            ids = jdbcTemplate.queryForList(sql, Long.class);
        } catch (DataAccessException e) {
            // Una comilla suelta en username/password arma SQL invalido: 401 igual que una
            // credencial incorrecta, sin loguear la excepcion (el mensaje trae la consulta con
            // la contrasena en texto plano).
            throw new UnauthorizedException("Usuario o clave incorrectos");
        }
        if (ids.isEmpty()) {
            throw new UnauthorizedException("Usuario o clave incorrectos");
        }
        User user = userRepository.findById(ids.get(0))
                .orElseThrow(() -> new UnauthorizedException("Usuario o clave incorrectos"));
        return mapper.toSession(user);
    }

}
