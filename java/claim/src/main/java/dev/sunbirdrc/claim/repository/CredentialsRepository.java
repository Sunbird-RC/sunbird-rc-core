package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials,Long> {

}
