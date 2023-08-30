package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearnerRepository extends JpaRepository<Learner,Long> {
    List<Learner> findByName(String name);
    List<Learner> findByRollNumber(String rollNumber);

}
