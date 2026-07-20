package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentControllerUnitTest {

    static int testStudentId;
    static int testEnrollmentId;

    static final String testStudentName = "Student Controller Test";
    static final String testStudentEmail = "student-controller@csumb.edu";
    static final String testStudentPassword = "test";

    @Autowired
    private WebTestClient client;

    @BeforeAll
    public static void addTestData(
            @Autowired UserRepository userRepository,
            @Autowired SectionRepository sectionRepository,
            @Autowired EnrollmentRepository enrollmentRepository
    ) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // create a student for the schedule and transcript tests
        User student = new User();
        student.setName(testStudentName);
        student.setEmail(testStudentEmail);
        student.setPassword(passwordEncoder.encode(testStudentPassword));
        student.setType("STUDENT");
        userRepository.save(student);

        testStudentId = student.getId();

        // enroll the test student in the existing section
        Section section = sectionRepository.findById(1).orElseThrow();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade("A");
        enrollmentRepository.save(enrollment);

        testEnrollmentId = enrollment.getEnrollmentId();
    }

    @AfterAll
    public static void removeTestData(
            @Autowired UserRepository userRepository,
            @Autowired EnrollmentRepository enrollmentRepository
    ) {
        enrollmentRepository.deleteById(testEnrollmentId);
        userRepository.deleteById(testStudentId);
    }

    @Test
    public void getStudentScheduleTest() {
        String jwt = login(testStudentEmail, testStudentPassword);

        EntityExchangeResult<List<EnrollmentDTO>> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/enrollments")
                        .queryParam("year", 2026)
                        .queryParam("semester", "Fall")
                        .build())
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();

        List<EnrollmentDTO> schedule = response.getResponseBody();

        assertNotNull(schedule);
        assertEquals(1, schedule.size());

        EnrollmentDTO enrollment = schedule.get(0);

        assertEquals(testEnrollmentId, enrollment.enrollmentId());
        assertEquals("A", enrollment.grade());
        assertEquals(testStudentId, enrollment.studentId());
        assertEquals(testStudentName, enrollment.name());
        assertEquals(testStudentEmail, enrollment.email());
        assertEquals("cst489", enrollment.courseId());
        assertEquals("Software Engineering", enrollment.title());
        assertEquals(1, enrollment.sectionId());
        assertEquals(1, enrollment.sectionNo());
        assertEquals("90", enrollment.building());
        assertEquals("B104", enrollment.room());
        assertEquals("W F 10-11", enrollment.times());
        assertEquals(4, enrollment.credits());
        assertEquals(2026, enrollment.year());
        assertEquals("Fall", enrollment.semester());
    }

    @Test
    public void getStudentScheduleEmptyTermTest() {
        String jwt = login(testStudentEmail, testStudentPassword);

        EntityExchangeResult<List<EnrollmentDTO>> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/enrollments")
                        .queryParam("year", 2025)
                        .queryParam("semester", "Spring")
                        .build())
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();

        List<EnrollmentDTO> schedule = response.getResponseBody();

        assertNotNull(schedule);
        assertTrue(schedule.isEmpty());
    }

    @Test
    public void getStudentTranscriptTest() {
        String jwt = login(testStudentEmail, testStudentPassword);

        EntityExchangeResult<List<EnrollmentDTO>> response = client.get()
                .uri("/transcripts")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();

        List<EnrollmentDTO> transcript = response.getResponseBody();

        assertNotNull(transcript);
        assertEquals(1, transcript.size());

        EnrollmentDTO enrollment = transcript.get(0);

        assertEquals(testEnrollmentId, enrollment.enrollmentId());
        assertEquals("A", enrollment.grade());
        assertEquals(testStudentId, enrollment.studentId());
        assertEquals(testStudentName, enrollment.name());
        assertEquals(testStudentEmail, enrollment.email());
        assertEquals("cst489", enrollment.courseId());
        assertEquals("Software Engineering", enrollment.title());
        assertEquals(1, enrollment.sectionId());
        assertEquals(1, enrollment.sectionNo());
        assertEquals("90", enrollment.building());
        assertEquals("B104", enrollment.room());
        assertEquals("W F 10-11", enrollment.times());
        assertEquals(4, enrollment.credits());
        assertEquals(2026, enrollment.year());
        assertEquals("Fall", enrollment.semester());
    }

    @Test
    public void getTranscriptRequiresStudentLoginTest() {
        String adminJwt = login("admin@csumb.edu", "admin");

        client.get()
                .uri("/transcripts")
                .headers(headers -> headers.setBearerAuth(adminJwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden();
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