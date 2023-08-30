package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
