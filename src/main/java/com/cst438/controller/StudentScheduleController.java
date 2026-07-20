package com.cst438.controller;

import com.cst438.domain.Course;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.Term;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.GradebookServiceProxy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Date;

@RestController
public class StudentScheduleController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final GradebookServiceProxy gradebook;

    public StudentScheduleController(
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            UserRepository userRepository,
            GradebookServiceProxy gradebook
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.gradebook = gradebook;
    }

    @PostMapping("/enrollments/sections/{sectionNo}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public EnrollmentDTO addCourse(
            @PathVariable int sectionNo,
            Principal principal) throws Exception {

        // create and save an EnrollmentEntity
        //  relate enrollment to the student's User entity and to the Section entity
        //  check that student is not already enrolled in the section
        //  check that the current date is not before addDate, not after addDeadline
        //  of the section's term.  Return an EnrollmentDTO with the id of the
        //  Enrollment and other fields.

        User student = userRepository.findByEmail(principal.getName());

        if (student == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "student not found"
            );
        }

        Section section = sectionRepository.findById(sectionNo).orElse(null);

        if (section == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "section not found"
            );
        }

        Enrollment currentEnrollment =
                enrollmentRepository.findEnrollmentBySectionNoAndStudentId(
                        sectionNo,
                        student.getId()
                );

        if (currentEnrollment != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "student is already enrolled in this section"
            );
        }

        Term term = section.getTerm();
        Date today = new Date();

        if (today.before(term.getAddDate())
                || today.after(term.getAddDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "section is not open for enrollment"
            );
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        Course course = section.getCourse();

        EnrollmentDTO result = new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                course.getCourseId(),
                course.getTitle(),
                section.getSectionId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                course.getCredits(),
                term.getYear(),
                term.getSemester()
        );

        gradebook.sendMessage("addEnrollment", result);

        return result;
    }

    // student drops a course
    @DeleteMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public void dropCourse(
            @PathVariable("enrollmentId") int enrollmentId,
            Principal principal) throws Exception {

        // check that enrollment belongs to the logged in student
        // and that today is not after the dropDeadLine for the term.

        User student = userRepository.findByEmail(principal.getName());

        if (student == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "student not found"
            );
        }

        Enrollment enrollment =
                enrollmentRepository.findById(enrollmentId).orElse(null);

        if (enrollment == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "enrollment not found"
            );
        }

        if (enrollment.getStudent().getId() != student.getId()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "enrollment does not belong to student"
            );
        }

        Term term = enrollment.getSection().getTerm();
        Date today = new Date();

        if (today.after(term.getDropDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "drop deadline has passed"
            );
        }

        enrollmentRepository.deleteById(enrollmentId);
        gradebook.sendMessage("deleteEnrollment", enrollmentId);
    }
}