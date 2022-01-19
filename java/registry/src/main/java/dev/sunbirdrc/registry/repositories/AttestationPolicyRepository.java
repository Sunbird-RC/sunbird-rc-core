package dev.sunbirdrc.registry.repositories;

import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.entities.AttestationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttestationPolicyRepository extends JpaRepository<AttestationPolicy, String> {
    List<AttestationPolicy> findAllByEntityAndStatus(String name, AttestationStatus status);
    List<AttestationPolicy> findAllByEntity(String name);
    List<AttestationPolicy> findAllByEntityAndCreatedBy(String name, String createdBy);
}
