package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.entity.Courses;
import dev.sunbirdrc.claim.repository.CoursesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CoursesService {

    private CoursesRepository coursesRepository;

    @Autowired
    public CoursesService(CoursesRepository coursesRepository) {
        this.coursesRepository = coursesRepository;
    }

    public List<Courses> getAllCourses() {
        return coursesRepository.findAll();
    }

    public Optional<Courses> getCourseById(Long id) {
        return coursesRepository.findById(id);
    }

    public Optional<Courses> getCourseByCourse(String category) {
        return coursesRepository.findByCategory(category);
    }

    // find Couse Name
    public List<String> getCourseByCategory(String category) {
        return coursesRepository.findByFieldName(category);
    }

    //find TemplateKey
    public String getCourseTemplateKey(String courseName) {
        return coursesRepository.findByCouseName(courseName);
    }
    public Courses createCourse(Courses course) {
        return coursesRepository.save(course);
    }


}
