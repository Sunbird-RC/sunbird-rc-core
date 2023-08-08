package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    boolean existsByEmailId(String emailId);
}
