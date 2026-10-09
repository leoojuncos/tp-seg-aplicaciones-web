package ar.edu.utn.frba.tps.monolith.auth.mapper;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.model.Permission;
import ar.edu.utn.frba.tps.monolith.auth.model.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public SessionResponse toSession(User user) {
        return new SessionResponse(
                user.getUsername(),
                user.getRole().getCode(),
                user.getPermissions().stream().map(Permission::getCode).sorted().toList());
    }

}
