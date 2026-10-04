package ar.edu.utn.frba.tps.monolith.messaging.mapper;

import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalAccountResponse;
import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalSessionResponse;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalSession;
import org.springframework.stereotype.Component;

@Component
public class MessagingMapper {

    public TechnicalAccountResponse toResponse(TechnicalAccount account) {
        return new TechnicalAccountResponse(account.getUsername(), account.getRole(), account.getServiceId());
    }

    public TechnicalSessionResponse toResponse(TechnicalSession session) {
        return new TechnicalSessionResponse(session.token(), session.username(), session.role(), session.expiresAt());
    }

}
