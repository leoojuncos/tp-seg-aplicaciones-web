package ar.edu.utn.frba.tps.monolith.auth.repository;

import ar.edu.utn.frba.tps.monolith.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
