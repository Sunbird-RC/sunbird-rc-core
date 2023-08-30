package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.ClaimUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimUserRepository extends JpaRepository<ClaimUser, Long> {
}
