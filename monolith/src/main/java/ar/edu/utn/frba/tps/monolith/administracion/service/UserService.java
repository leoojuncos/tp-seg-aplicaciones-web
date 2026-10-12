package ar.edu.utn.frba.tps.monolith.administracion.service;

import ar.edu.utn.frba.tps.monolith.administracion.dto.UserDetailResponse;
import ar.edu.utn.frba.tps.monolith.administracion.dto.UserResponse;
import ar.edu.utn.frba.tps.monolith.administracion.mapper.AdministracionMapper;
import ar.edu.utn.frba.tps.monolith.auth.repository.UserRepository;
import ar.edu.utn.frba.tps.monolith.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consulta de solo lectura de los usuarios y sus permisos. Es de solo lectura a proposito: dejar
 * asignar permisos o cambiar contrasenas desde aca seria un atajo para el paso 1 del escenario.
 * Importa las entidades y el repositorio del modulo auth; no las vuelve a crear.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AdministracionMapper mapper;

    @Autowired
    public UserService(UserRepository userRepository, AdministracionMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by("username")).stream()
                .map(mapper::toResponse)
                .toList();
    }

    // Transaccional porque el detalle recorre los permisos (una coleccion lazy del usuario).
    @Transactional(readOnly = true)
    public UserDetailResponse get(Long id) {
        return userRepository.findById(id)
                .map(mapper::toDetail)
                .orElseThrow(() -> new NotFoundException("No existe el usuario " + id));
    }

}
