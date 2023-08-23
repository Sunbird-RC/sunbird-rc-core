package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.entity.Courses;
import dev.sunbirdrc.claim.service.CoursesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/courses")
public class CoursesController {

    private CoursesService coursesService;

    @Autowired
    public CoursesController(CoursesService coursesService) {
        this.coursesService = coursesService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Courses>> getAllCourses() {
        List<Courses> courses = coursesService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Courses> getCourseById(@PathVariable Long id) {
        Optional<Courses> course = coursesService.getCourseById(id);

        if (course.isPresent()) {
            return ResponseEntity.ok(course.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Courses> getCourseByName(@PathVariable String category) {
        Optional<Courses> course = coursesService.getCourseByCourse(category);

        if (course.isPresent()) {
            return ResponseEntity.ok(course.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/diploma/{category}")
    public ResponseEntity<List<String>> getCourseByCategory(@PathVariable String category) {
        List<String> course = coursesService.getCourseByCategory(category);
            return ResponseEntity.ok(course);
    }

    @GetMapping("/course-short-name/{courseName}")
    public ResponseEntity<String> getCourseShortName(@PathVariable String courseName) {
        courseName = courseName.replace(" ","");
        String course = coursesService.getCourseShortName(courseName);
        return ResponseEntity.ok(course);
    }

    @PostMapping
    public ResponseEntity<Courses> createCourse(@RequestBody Courses course) {
        Courses savedCourse = coursesService.createCourse(course);
        return ResponseEntity.ok(savedCourse);
    }
}
