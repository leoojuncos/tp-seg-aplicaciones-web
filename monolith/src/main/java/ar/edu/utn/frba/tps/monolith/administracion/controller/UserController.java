package ar.edu.utn.frba.tps.monolith.administracion.controller;

import ar.edu.utn.frba.tps.monolith.administracion.dto.UserDetailResponse;
import ar.edu.utn.frba.tps.monolith.administracion.dto.UserResponse;
import ar.edu.utn.frba.tps.monolith.administracion.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/administracion/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    public UserDetailResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

}
