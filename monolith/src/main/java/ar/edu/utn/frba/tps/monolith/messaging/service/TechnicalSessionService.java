package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emite y valida las sesiones propias del modulo: tokens opacos y aleatorios, guardados en memoria
 * con vencimiento. Un reinicio del monolito invalida todas las sesiones.
 */
@Service
public class TechnicalSessionService {

    private static final int TOKEN_BYTES = 32;

    private final Map<String, TechnicalSession> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public TechnicalSessionService(MessagingProperties properties) {
        this(properties.sessionTtl(), Clock.systemUTC());
    }

    TechnicalSessionService(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public TechnicalSession issue(TechnicalAccount account) {
        Instant now = clock.instant();
        sessions.values().removeIf(session -> session.isExpiredAt(now));

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TechnicalSession session = new TechnicalSession(token, account.getUsername(), account.getRole(), now.plus(ttl));
        sessions.put(token, session);
        return session;
    }

    public Optional<TechnicalSession> resolve(String token) {
        TechnicalSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpiredAt(clock.instant())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

}
