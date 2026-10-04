package ar.edu.utn.frba.tps.monolith.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El seed copia de docs/contracts.md los hashes de las cuentas tecnicas. El de la cuenta de lectura
 * tiene que corresponder a su contrasena publicada, que tambien usan Auditoria y el docker-compose. La
 * cuenta de escritura publica solo el hash: su contrasena no tiene que poder obtenerse por fuera de la
 * recuperacion de la cuenta.
 */
class TechnicalAccountsContractTest {

    private static final Path CONTRACTS = Path.of("..", "docs", "contracts.md");
    private static final String BCRYPT = "\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}";

    @Test
    void readAccountHashMatchesItsPublishedPassword() throws IOException {
        String[] reader = row("auditoria_lector");

        assertThat(new BCryptPasswordEncoder().matches(unquote(reader[3]), unquote(reader[5]))).isTrue();
    }

    @Test
    void writeAccountPublishesOnlyItsHash() throws IOException {
        String[] operator = row("auditoria_operador");

        assertThat(operator[3]).doesNotContain("`");
        assertThat(unquote(operator[5])).matches(BCRYPT);
    }

    private static String[] row(String account) throws IOException {
        return Files.readAllLines(CONTRACTS).stream()
                .filter(line -> line.startsWith("| `" + account + "`"))
                .map(line -> line.split("\\|"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta la fila de " + account + " en docs/contracts.md"));
    }

    private static String unquote(String cell) {
        return cell.strip().replace("`", "");
    }

}
