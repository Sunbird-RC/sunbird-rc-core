package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.GeneratedNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedNumberRepository extends JpaRepository<GeneratedNumber, Long> {
}
