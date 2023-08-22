package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Courses;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursesRepository extends JpaRepository<Courses, Long> {
}

