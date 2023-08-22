package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Claim;
import dev.sunbirdrc.claim.entity.Regulator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//@Repository
public interface RegulatorRepository {//extends JpaRepository<Regulator, String> {
    List<Regulator> findByCouncil(String council);

    List<Regulator> findByCouncilNot(String council);
}
