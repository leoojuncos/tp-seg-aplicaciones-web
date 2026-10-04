package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalAccountResponse;
import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalSessionResponse;
import ar.edu.utn.frba.tps.monolith.messaging.exception.MessagingUnauthorizedException;
import ar.edu.utn.frba.tps.monolith.messaging.mapper.MessagingMapper;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.repository.TechnicalAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TechnicalAccountService {

    private final TechnicalAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TechnicalSessionService sessionService;
    private final MessagingMapper mapper;
    private final String unknownUserHash;

    @Autowired
    public TechnicalAccountService(TechnicalAccountRepository accountRepository, PasswordEncoder passwordEncoder,
                                   TechnicalSessionService sessionService, MessagingMapper mapper) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.mapper = mapper;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public TechnicalSessionResponse login(String username, String password) {
        Optional<TechnicalAccount> account = accountRepository.findByUsername(username);
        // Se compara contra un hash aunque el usuario no exista, para que el tiempo de respuesta
        // no revele que cuentas existen.
        String hash = account.map(TechnicalAccount::getPasswordHash).orElse(unknownUserHash);
        if (!passwordEncoder.matches(password, hash) || account.isEmpty()) {
            throw new MessagingUnauthorizedException("Usuario o clave incorrectos");
        }
        return mapper.toResponse(sessionService.issue(account.get()));
    }

    public List<TechnicalAccountResponse> listAccounts() {
        return accountRepository.findAll(Sort.by("username")).stream()
                .map(mapper::toResponse)
                .toList();
    }

}
