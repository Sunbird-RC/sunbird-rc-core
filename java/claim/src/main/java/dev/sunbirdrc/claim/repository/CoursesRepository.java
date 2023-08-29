package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Course;
import dev.sunbirdrc.claim.entity.Courses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoursesRepository extends JpaRepository<Courses, Long> {
    Optional<Courses> findByCategory(String category);
    @Query("SELECT c.courseName FROM Courses c WHERE c.category = :value")
    List<String> findByFieldName(@Param("value") String value);

    @Query("SELECT c.courseNameKey FROM Courses c WHERE c.courseName = :value")
    String findByCouseName(@Param("value") String value);


}

