package ar.edu.utn.frba.tps.monolith.messaging.repository;

import ar.edu.utn.frba.tps.monolith.messaging.model.PendingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PendingEventRepository extends JpaRepository<PendingEvent, UUID> {

    List<PendingEvent> findAllByOrderByOccurredAtDesc();

}
