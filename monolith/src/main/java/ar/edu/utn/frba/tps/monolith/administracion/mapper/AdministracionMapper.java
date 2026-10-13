package ar.edu.utn.frba.tps.monolith.administracion.mapper;

import ar.edu.utn.frba.tps.monolith.administracion.dto.UserDetailResponse;
import ar.edu.utn.frba.tps.monolith.administracion.dto.UserResponse;
import ar.edu.utn.frba.tps.monolith.auth.model.Permission;
import ar.edu.utn.frba.tps.monolith.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdministracionMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().getCode());
    }

    public UserDetailResponse toDetail(User user) {
        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getCode)
                .sorted()
                .toList();
        return new UserDetailResponse(user.getId(), user.getUsername(), user.getRole().getCode(), permissions);
    }

}
