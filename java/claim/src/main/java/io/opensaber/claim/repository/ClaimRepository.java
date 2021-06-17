package io.opensaber.claim.repository;

import io.opensaber.claim.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, String> {
    List<Claim> findByConditionsIn(List<String> conditions);
    List<Claim> findByAttestorEntityIn(List<String> entities);
    List<Claim> findByAttestorEntity(String entity);
}
