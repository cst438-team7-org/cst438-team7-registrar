package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.OpenSectionDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.dto.UserDTO;
import com.cst438.service.GradebookServiceProxy;
import jakarta.validation.Valid;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class SectionController {

    private final CourseRepository courseRepository;

    private final SectionRepository sectionRepository;

    private final TermRepository termRepository;

    private final UserRepository userRepository;

    private final GradebookServiceProxy gradebook;

    private final EnrollmentRepository enrollmentRepository;

    public SectionController(
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            TermRepository termRepository,
            UserRepository userRepository,
            GradebookServiceProxy gradebook,
            EnrollmentRepository enrollmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.termRepository = termRepository;
        this.userRepository = userRepository;
        this.gradebook = gradebook;
        this.enrollmentRepository = enrollmentRepository;
    }


    // ADMIN function to create a new section
    @PostMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public SectionDTO addSection(@Valid @RequestBody SectionDTO dto) throws Exception {

        // check that instructorEmail exists in database and is an INSTRUCTOR
        // create a Section entity related to course
        // return SectionDTO include the database generated primary key value
        // send message to GradeService using gradebook proxy
        User u = userRepository.findByEmail(dto.instructorEmail());
        if (u==null || !u.getType().equals("INSTRUCTOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }
        Course c = courseRepository.findById(dto.courseId()).orElse(null);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid course id");
        }
        Term t = termRepository.findByYearAndSemester(dto.year(), dto.semester());
        if (t==null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid term");
        }
        Section s = new Section();
        s.setCourse(c);
        s.setTerm(t);
        s.setSectionId(dto.secId());
        s.setBuilding(dto.building());
        s.setRoom(dto.room());
        s.setTimes(dto.times());
        s.setInstructorEmail(u.getEmail());
        s.setCapacity(30);
        sectionRepository.save(s);
        SectionDTO result = new SectionDTO(
                s.getSectionNo(),
                t.getYear(),
                t.getSemester(),
                dto.courseId(),
                c.getTitle(),
                s.getSectionId(),
                s.getBuilding(),
                s.getRoom(),
                s.getTimes(),
                u.getName(),
                s.getInstructorEmail()
        );
        gradebook.sendMessage("addSection", result);
        return result;
    }

    // ADMIN function to update a section
    @PutMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public SectionDTO updateSection(@Valid @RequestBody SectionDTO dto) throws Exception {
        //  validate and update fields instructorEmail
        //  update only fields instructorEmail, building, room, times, sectionId
        //  send message to GradeService using gradebook proxy
        Section s = sectionRepository.findById(dto.secNo()).orElse(null);
        if (s==null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "section not found");
        }
        User u = userRepository.findByEmail(dto.instructorEmail());
        if (u==null || !u.getType().equals("INSTRUCTOR")) {
            BindingResult bindingResult = new BeanPropertyBindingResult(dto, "SectionDTO");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }
        s.setSectionId(dto.secId());
        s.setBuilding(dto.building());
        s.setRoom(dto.room());
        s.setTimes(dto.times());
        s.setInstructorEmail(dto.instructorEmail());
        sectionRepository.save(s);
        SectionDTO result = new SectionDTO(
                s.getSectionNo(),
                s.getTerm().getYear(),
                s.getTerm().getSemester(),
                dto.courseId(),
                s.getCourse().getTitle(),
                s.getSectionId(),
                s.getBuilding(),
                s.getRoom(),
                s.getTimes(),
                u.getName(),
                s.getInstructorEmail()
        );
        gradebook.sendMessage("updateSection", result);
        return result;
    }

    // ADMIN function to create a delete section
    // delete will fail if there are related assignments or enrollments
    @DeleteMapping("/sections/{sectionno}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public void deleteSection(@PathVariable int sectionno) {
        // delete the section entity
        //  send message to GradeService using gradebook proxy
        sectionRepository.deleteById(sectionno);
        gradebook.sendMessage("deleteSection", sectionno);
    }


    // returns Sections for a course and term
    // courseId may be partial
    @GetMapping("/courses/{courseId}/sections")
    public List<SectionDTO> getSections(
            @PathVariable("courseId") String courseId,
            @RequestParam("year") int year ,
            @RequestParam("semester") String semester )  {

        return sectionRepository.findByLikeCourseIdAndYearAndSemester(courseId+"%", year, semester)
                .stream()
                .map(s -> {
                    User instructor = userRepository.findByEmail(s.getInstructorEmail());
                    return new SectionDTO(
                            s.getSectionNo(),
                            s.getTerm().getYear(),
                            s.getTerm().getSemester(),
                            s.getCourse().getCourseId(),
                            s.getCourse().getTitle(),
                            s.getSectionId(),
                            s.getBuilding(),
                            s.getRoom(),
                            s.getTimes(),
                            instructor.getName(),
                            s.getInstructorEmail()
                    );
                }).toList();
    }

    // sections that are open for enrollment
    // today's date not before addDate and not after addDeadline
    @GetMapping("/sections/open")
    public List<OpenSectionDTO> getOpenSectionsForEnrollment() {

        return sectionRepository.findByOpenOrderByCourseIdSectionId().stream().map( s -> {
            User instructor = userRepository.findByEmail(s.getInstructorEmail());

            long enrolledSeats =
                    enrollmentRepository.countBySectionNo(s.getSectionNo());

            long availableSeats =
                    Math.max(s.getCapacity() - enrolledSeats, 0);

            return new OpenSectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSectionId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    instructor.getName(),
                    s.getInstructorEmail(),
                    s.getCapacity(),
                    enrolledSeats,
                    availableSeats
            );
        }).toList();
    }
}