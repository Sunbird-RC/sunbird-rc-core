package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.TelemetryObject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryObjectRepository extends JpaRepository<TelemetryObject, Long> {
}
