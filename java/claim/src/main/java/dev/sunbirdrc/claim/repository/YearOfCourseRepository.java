package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.YearsOfCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YearOfCourseRepository extends JpaRepository<YearsOfCourse, Long> {
}
