package ar.edu.utn.frba.tps.monolith.auth.repository;

import ar.edu.utn.frba.tps.monolith.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
