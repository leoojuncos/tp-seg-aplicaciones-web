package ar.edu.utn.frba.tps.monolith.auth.repository;

import ar.edu.utn.frba.tps.monolith.auth.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
