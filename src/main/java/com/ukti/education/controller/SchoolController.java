package com.ukti.education.controller;

import com.ukti.education.dto.AddStudentsRequest;
import com.ukti.education.dto.AddStudentsResponse;
import com.ukti.education.dto.AddTeacherRequest;
import com.ukti.education.dto.ClassCreateRequest;
import com.ukti.education.dto.ClassLeaderboardResponse;
import com.ukti.education.dto.ClassResponse;
import com.ukti.education.dto.LastActivityResponse;
import com.ukti.education.dto.SchoolProgressOverviewResponse;
import com.ukti.education.dto.StudentResponse;
import com.ukti.education.dto.TeacherResponse;
import com.ukti.education.service.SchoolAuthService;
import com.ukti.education.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/schools")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5172", "https://miraista.com", "http://miraista.com", "https://www.miraista.com", "http://www.miraista.com", "https://education.miraista.com", "http://education.miraista.com", "https://educationuat.miraista.com", "http://educationuat.miraista.com", "https://ukti.example.com"})
public class SchoolController {

    private final SchoolAuthService schoolAuthService;
    private final SchoolService schoolService;

    @GetMapping("/{schoolId}/classes")
    public ResponseEntity<?> listClasses(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        List<ClassResponse> classes = schoolService.listClasses(schoolId);
        return ResponseEntity.ok(classes);
    }

    @PostMapping("/{schoolId}/classes")
    public ResponseEntity<?> createClass(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @Valid @RequestBody ClassCreateRequest request) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        if (auth.get().isTeacher()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Only school admin can create classes"));
        }

        try {
            ClassResponse created = schoolService.createClass(schoolId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    @PostMapping("/{schoolId}/classes/{classId}/students")
    public ResponseEntity<?> addStudents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @Valid @RequestBody AddStudentsRequest request) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        if (auth.get().isTeacher()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Only school admin can add students"));
        }

        AddStudentsResponse response = schoolService.addStudents(schoolId, classId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{schoolId}/classes/{classId}/students")
    public ResponseEntity<?> listStudents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        List<StudentResponse> students = schoolService.listStudents(schoolId, classId);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{schoolId}/teachers")
    public ResponseEntity<?> listTeachers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        List<TeacherResponse> teachers = schoolService.listTeachers(schoolId);
        return ResponseEntity.ok(teachers);
    }

    @PostMapping("/{schoolId}/teachers")
    public ResponseEntity<?> addTeacher(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @Valid @RequestBody AddTeacherRequest request) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        if (auth.get().isTeacher()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Only school admin can add teachers"));
        }

        try {
            TeacherResponse created = schoolService.addTeacher(schoolId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    /**
     * Get progress overview for school admin dashboard.
     * Returns admin's progress + all students' progress by class in one call.
     * Optional query param: totalActivities (e.g. 45) for percent calculation.
     */
    @GetMapping("/{schoolId}/progress/overview")
    public ResponseEntity<?> getProgressOverview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @RequestParam(value = "totalActivities", required = false) Integer totalActivities) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        SchoolAuthService.SchoolAuthContext ctx = auth.get();
        java.util.Set<UUID> classIdsToInclude = ctx.isTeacher() && ctx.teacherId() != null
                ? schoolService.getTeacherClassIds(ctx.teacherId())
                : null;
        SchoolProgressOverviewResponse response = schoolService.getProgressOverview(
                schoolId, ctx.adminOrTeacherUserId(), totalActivities, classIdsToInclude);
        return ResponseEntity.ok(response);
    }

    /**
     * Last activity completed by any student in this class.
     * 404 if no activity in class.
     */
    @GetMapping("/{schoolId}/classes/{classId}/last-activity")
    public ResponseEntity<?> getLastActivity(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        SchoolAuthService.SchoolAuthContext ctx = auth.get();
        if (!schoolService.canAccessClass(schoolId, classId, ctx.isTeacher() ? ctx.teacherId() : null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Class not found or access denied"));
        }

        Optional<LastActivityResponse> response = schoolService.getLastActivity(schoolId, classId);
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Class leaderboard: top students by completed activities count.
     * limit defaults to 3.
     */
    @GetMapping("/{schoolId}/classes/{classId}/leaderboard")
    public ResponseEntity<?> getClassLeaderboard(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @RequestParam(value = "limit", defaultValue = "3") int limit) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        SchoolAuthService.SchoolAuthContext ctx = auth.get();
        if (!schoolService.canAccessClass(schoolId, classId, ctx.isTeacher() ? ctx.teacherId() : null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Class not found or access denied"));
        }

        ClassLeaderboardResponse response = schoolService.getClassLeaderboard(schoolId, classId, limit);
        return ResponseEntity.ok(response);
    }
}
