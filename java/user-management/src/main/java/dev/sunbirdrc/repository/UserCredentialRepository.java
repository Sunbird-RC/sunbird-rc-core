package dev.sunbirdrc.repository;

import dev.sunbirdrc.entity.UserCredential;
import dev.sunbirdrc.entity.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUserName(String username);
}
