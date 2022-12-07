package dev.sunbirdrc.consent.repository;

import dev.sunbirdrc.consent.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, String> {
}
