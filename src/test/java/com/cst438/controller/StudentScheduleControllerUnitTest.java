package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.Term;
import com.cst438.domain.TermRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.GradebookServiceProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentScheduleControllerUnitTest {

    static final int testSectionNo = 1;
    static final String testStudentName = "Schedule Test Student";
    static final String testStudentEmail = "schedule-test@csumb.edu";
    static final String testStudentPassword = "test";

    int testStudentId;
    Date originalAddDate;
    Date originalAddDeadline;
    Date originalDropDeadline;

    @Autowired
    private WebTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TermRepository termRepository;

    @MockitoBean
    private GradebookServiceProxy gradebookService;

    @BeforeEach
    public void addTestData() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // create a student for the enrollment tests
        User student = new User();
        student.setName(testStudentName);
        student.setEmail(testStudentEmail);
        student.setPassword(passwordEncoder.encode(testStudentPassword));
        student.setType("STUDENT");
        userRepository.save(student);

        testStudentId = student.getId();

        // make sure the existing section is open for enrollment and dropping
        Section section = sectionRepository.findById(testSectionNo).orElseThrow();
        Term term = section.getTerm();

        originalAddDate = term.getAddDate();
        originalAddDeadline = term.getAddDeadline();
        originalDropDeadline = term.getDropDeadline();

        term.setAddDate(Date.valueOf(LocalDate.now().minusDays(1)));
        term.setAddDeadline(Date.valueOf(LocalDate.now().plusDays(1)));
        term.setDropDeadline(Date.valueOf(LocalDate.now().plusDays(1)));
        termRepository.save(term);
    }

    @AfterEach
    public void removeTestData() {
        Enrollment enrollment =
                enrollmentRepository.findEnrollmentBySectionNoAndStudentId(
                        testSectionNo,
                        testStudentId
                );

        if (enrollment != null) {
            enrollmentRepository.deleteById(enrollment.getEnrollmentId());
        }

        userRepository.deleteById(testStudentId);

        Section section = sectionRepository.findById(testSectionNo).orElseThrow();
        Term term = section.getTerm();

        term.setAddDate(originalAddDate);
        term.setAddDeadline(originalAddDeadline);
        term.setDropDeadline(originalDropDeadline);
        termRepository.save(term);
    }

    @Test
    public void addCourseTest() {
        String jwt = login(testStudentEmail, testStudentPassword);

        EntityExchangeResult<EnrollmentDTO> response = client.post()
                .uri("/enrollments/sections/" + testSectionNo)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO.class)
                .returnResult();

        EnrollmentDTO enrollmentDTO = response.getResponseBody();

        assertNotNull(enrollmentDTO);
        assertTrue(enrollmentDTO.enrollmentId() > 0);
        assertNull(enrollmentDTO.grade());
        assertEquals(testStudentId, enrollmentDTO.studentId());
        assertEquals(testStudentName, enrollmentDTO.name());
        assertEquals(testStudentEmail, enrollmentDTO.email());
        assertEquals("cst489", enrollmentDTO.courseId());
        assertEquals("Software Engineering", enrollmentDTO.title());
        assertEquals(1, enrollmentDTO.sectionId());
        assertEquals(testSectionNo, enrollmentDTO.sectionNo());
        assertEquals("90", enrollmentDTO.building());
        assertEquals("B104", enrollmentDTO.room());
        assertEquals("W F 10-11", enrollmentDTO.times());
        assertEquals(4, enrollmentDTO.credits());
        assertEquals(2026, enrollmentDTO.year());
        assertEquals("Fall", enrollmentDTO.semester());

        // check that the enrollment was saved in the database
        Enrollment enrollment =
                enrollmentRepository.findEnrollmentBySectionNoAndStudentId(
                        testSectionNo,
                        testStudentId
                );

        assertNotNull(enrollment);
        assertEquals(enrollmentDTO.enrollmentId(), enrollment.getEnrollmentId());

        // check that the enrollment was sent to the gradebook
        verify(gradebookService, times(1))
                .sendMessage(eq("addEnrollment"), any());
    }

    @Test
    public void addDuplicateCourseTest() {
        User student = userRepository.findByEmail(testStudentEmail);
        Section section = sectionRepository.findById(testSectionNo).orElseThrow();

        // student is already enrolled in the section
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        String jwt = login(testStudentEmail, testStudentPassword);

        client.post()
                .uri("/enrollments/sections/" + testSectionNo)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        verify(gradebookService, never())
                .sendMessage(eq("addEnrollment"), any());
    }

    @Test
    public void dropCourseTest() {
        User student = userRepository.findByEmail(testStudentEmail);
        Section section = sectionRepository.findById(testSectionNo).orElseThrow();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        int enrollmentId = enrollment.getEnrollmentId();
        String jwt = login(testStudentEmail, testStudentPassword);

        client.delete()
                .uri("/enrollments/" + enrollmentId)
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk();

        // check that the enrollment was deleted from the database
        assertFalse(enrollmentRepository.findById(enrollmentId).isPresent());

        // check that the deletion was sent to the gradebook
        verify(gradebookService, times(1))
                .sendMessage("deleteEnrollment", enrollmentId);
    }

    @Test
    public void dropCourseAfterDeadlineTest() {
        User student = userRepository.findByEmail(testStudentEmail);
        Section section = sectionRepository.findById(testSectionNo).orElseThrow();
        Term term = section.getTerm();

        term.setDropDeadline(Date.valueOf(LocalDate.now().minusDays(1)));
        termRepository.save(term);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        int enrollmentId = enrollment.getEnrollmentId();
        String jwt = login(testStudentEmail, testStudentPassword);

        client.delete()
                .uri("/enrollments/" + enrollmentId)
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isBadRequest();

        // check that the enrollment was not deleted
        assertTrue(enrollmentRepository.findById(enrollmentId).isPresent());

        verify(gradebookService, never())
                .sendMessage(eq("deleteEnrollment"), any());
    }

    private String login(String email, String password) {
        EntityExchangeResult<LoginDTO> response = client.get()
                .uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        LoginDTO loginDTO = response.getResponseBody();

        assertNotNull(loginDTO);
        assertNotNull(loginDTO.jwt());

        return loginDTO.jwt();
    }
}