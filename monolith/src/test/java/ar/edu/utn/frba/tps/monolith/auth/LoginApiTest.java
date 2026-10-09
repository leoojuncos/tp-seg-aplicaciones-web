package ar.edu.utn.frba.tps.monolith.auth;

import ar.edu.utn.frba.tps.monolith.auth.model.Permission;
import ar.edu.utn.frba.tps.monolith.auth.model.Role;
import ar.edu.utn.frba.tps.monolith.auth.model.User;
import ar.edu.utn.frba.tps.monolith.auth.repository.PermissionRepository;
import ar.edu.utn.frba.tps.monolith.auth.repository.RoleRepository;
import ar.edu.utn.frba.tps.monolith.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API de login contra Postgres real, con el esquema y el seed de db/init/ ya
 * aplicados. Inserta su propio usuario de prueba via los repositories (no depende del seed para
 * los casos normales), pero el test de inyeccion si depende de que ya exista al menos una fila
 * en users (la del seed).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginApiTest {

    private static final String USERNAME = "test_login_user";
    private static final String PASSWORD = "clave-correcta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // No @BeforeEach: cada test que necesita el usuario comun lo crea llamando a este metodo. El
    // test de la inyeccion apilada corre fuera de la transaccion de la clase (ver mas abajo) y
    // necesita datos propios y confirmados de verdad; si esto fuera @BeforeEach, tambien correria
    // sin transaccion para ese test y dejaria TEST_ROLE commiteado de verdad, rompiendo el resto.
    private void createUser() {
        Role role = roleRepository.save(new Role("TEST_ROLE"));
        Permission permission = permissionRepository.save(new Permission("TEST_PERMISSION"));
        userRepository.save(new User(USERNAME, passwordEncoder.encode(PASSWORD), role, Set.of(permission)));
    }

    @Test
    void validCredentialsReturnTheSessionAndSetTheCookie() throws Exception {
        createUser();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("TEST_ROLE"))
                .andExpect(jsonPath("$.permissions[0]").value("TEST_PERMISSION"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("sgm_session=")));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        createUser();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USERNAME, "clave-incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void unknownUsernameIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no-existe", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void incompleteBodyIsAValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void booleanInjectionInUsernameBypassesThePasswordCheck() throws Exception {
        User firstUser = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).get(0);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("' OR '1'='1' --", "cualquier-cosa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(firstUser.getUsername()))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("sgm_session=")));
    }

    @Test
    void aQuoteInUsernameIsUnauthorizedNotAServerError() throws Exception {
        // Comilla suelta: SQL invalido, no un bypass. Tiene que dar 401, no 500 (y no debe
        // loguear la contrasena -- eso no se puede comprobar desde el test, pero el catch de
        // DataAccessException en AuthService evita que el mensaje de la excepcion llegue al log).
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("O'Brien", "cualquier-cosa")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    // NOT_SUPPORTED: un DELETE inyectado que llegara a correr tiene que quedar confirmado de
    // verdad para que el test lo detecte, y AuthService.login tiene que manejar su propia
    // transaccion (SET TRANSACTION READ ONLY incluido) como en produccion. Suspendida
    // la transaccion de @Transactional de la clase (que nunca commitea), el usuario de este test
    // tiene que ser propio y confirmado de verdad -- si no, login() no lo veria.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void stackedDeleteInjectionDoesNotDeleteData() throws Exception {
        assertStackedInjectionDoesNotDelete(username -> "'; DELETE FROM users WHERE username = '" + username + "' --");
    }

    // Igual que el anterior, pero cerrando antes la transaccion de solo lectura con un COMMIT
    // apilado: el READ ONLY solo no alcanza, lo que lo frena es que no pase una segunda sentencia.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void stackedDeleteAfterCommitDoesNotDeleteData() throws Exception {
        assertStackedInjectionDoesNotDelete(username -> "'; COMMIT; DELETE FROM users WHERE username = '" + username + "' --");
    }

    // Sin ";", la escritura tiene que ir dentro de la unica sentencia. nextval avanza la secuencia
    // aunque la transaccion termine en rollback (las secuencias no son transaccionales), asi que lo
    // unico que la frena es el SET TRANSACTION READ ONLY de AuthService. NOT_SUPPORTED, como los de
    // arriba: login() maneja su propia transaccion, como en produccion.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void writeInsideTheSingleStatementIsRejected() throws Exception {
        Long before = debtsSequenceValue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("' OR (SELECT nextval(pg_get_serial_sequence('debts', 'id'))) > 0 --", "x")))
                .andExpect(status().isUnauthorized());

        assertThat(debtsSequenceValue()).isEqualTo(before);
    }

    // set_config(..., false) cambia la sesion de Postgres, no la transaccion: si el login
    // commiteara, el cambio quedaria pegado a esa conexion del pool. El bypass entra igual (es el
    // paso 1), pero AuthService termina siempre en rollback y el cambio se deshace.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void sessionSettingsChangedByTheInjectionDoNotOutliveTheLogin() throws Exception {
        String applicationName = "login_inyectado_" + System.nanoTime() % 1_000_000;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("' OR set_config('application_name', '" + applicationName + "', false) IS NOT NULL --", "x")))
                .andExpect(status().isOk());

        Integer sessions = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE application_name = ?", Integer.class, applicationName);
        assertThat(sessions).isZero();
    }

    // El monolito se conecta con un rol sin superusuario: la inyeccion no llega a funciones como
    // pg_read_file, y el error de permisos termina en el mismo 401 que una clave incorrecta.
    @Test
    void superuserFunctionsAreOutOfReach() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("' OR pg_read_file('PG_VERSION') IS NOT NULL --", "x")))
                .andExpect(status().isUnauthorized());
    }

    private Long debtsSequenceValue() {
        String sequence = jdbcTemplate.queryForObject("SELECT pg_get_serial_sequence('debts', 'id')", String.class);
        return jdbcTemplate.queryForObject("SELECT last_value FROM " + sequence, Long.class);
    }

    private void assertStackedInjectionDoesNotDelete(UnaryOperator<String> injectedUsername) throws Exception {
        String suffix = String.valueOf(System.nanoTime() % 1_000_000);
        String username = "stacked_test_user_" + suffix;
        Role role = roleRepository.save(new Role("STACKED_ROLE_" + suffix));
        User user = userRepository.save(new User(username, passwordEncoder.encode("x"), role, Set.of()));
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(injectedUsername.apply(username), "x")))
                    .andExpect(status().isUnauthorized());

            // El DELETE inyectado no tiene que haber corrido: el usuario de prueba sigue existiendo.
            assertThat(userRepository.existsById(user.getId())).isTrue();
        } finally {
            userRepository.deleteAllById(List.of(user.getId()));
            roleRepository.deleteById(role.getId());
        }
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginBody(username, password));
    }

    private record LoginBody(String username, String password) {
    }

}
