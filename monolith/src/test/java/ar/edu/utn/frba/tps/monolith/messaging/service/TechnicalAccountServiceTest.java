package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalSessionResponse;
import ar.edu.utn.frba.tps.monolith.messaging.exception.MessagingUnauthorizedException;
import ar.edu.utn.frba.tps.monolith.messaging.mapper.MessagingMapper;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;
import ar.edu.utn.frba.tps.monolith.messaging.repository.TechnicalAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalAccountServiceTest {

    // Costo bajo de BCrypt: el test no mide la fuerza del hash sino la logica del login.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TechnicalSessionService sessionService =
            new TechnicalSessionService(Duration.ofMinutes(30), Clock.systemUTC());

    @Mock
    private TechnicalAccountRepository accountRepository;

    private TechnicalAccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new TechnicalAccountService(accountRepository, passwordEncoder, sessionService, new MessagingMapper());
    }

    @Test
    void loginWithValidCredentialsIssuesASessionWithTheRoleOfTheAccount() {
        when(accountRepository.findByUsername("operador")).thenReturn(Optional.of(account("operador", "secreto", TechnicalRole.WRITE)));

        TechnicalSessionResponse session = accountService.login("operador", "secreto");

        assertThat(session.username()).isEqualTo("operador");
        assertThat(session.role()).isEqualTo(TechnicalRole.WRITE);
        assertThat(sessionService.resolve(session.token())).isPresent();
    }

    @Test
    void wrongPasswordAndUnknownUserAreRejectedTheSameWay() {
        when(accountRepository.findByUsername("operador")).thenReturn(Optional.of(account("operador", "secreto", TechnicalRole.WRITE)));
        when(accountRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        Throwable wrongPassword = catchThrowable(() -> accountService.login("operador", "otra"));
        Throwable unknownUser = catchThrowable(() -> accountService.login("nadie", "secreto"));

        assertThat(wrongPassword).isInstanceOf(MessagingUnauthorizedException.class);
        assertThat(unknownUser).isInstanceOf(MessagingUnauthorizedException.class)
                .hasMessage(wrongPassword.getMessage());
    }

    private TechnicalAccount account(String username, String password, TechnicalRole role) {
        return new TechnicalAccount(username, passwordEncoder.encode(password), role, "SERVICE");
    }

}
