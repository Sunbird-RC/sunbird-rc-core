package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.ClaimNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimNoteRepository extends JpaRepository<ClaimNote, String> {
    List<ClaimNote> findByEntityIdAndPropertyURI(String entityId, String propertyURI);
    List<ClaimNote> findByEntityIdAndClaimId(String entityId, String claimId);

}
