package com.ukti.education.controller;

import com.ukti.education.dto.GroupActivityCompleteRequest;
import com.ukti.education.dto.GroupActivityCompleteResponse;
import com.ukti.education.dto.GroupActivityHistoryResponse;
import com.ukti.education.dto.GroupActivityLeaderboardResponse;
import com.ukti.education.service.GroupActivityService;
import com.ukti.education.service.SchoolAuthService;
import com.ukti.education.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/progress/group-activity")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5172", "https://miraista.com", "http://miraista.com", "https://www.miraista.com", "http://www.miraista.com", "https://education.miraista.com", "http://education.miraista.com", "https://educationuat.miraista.com", "http://educationuat.miraista.com", "https://ukti.example.com"})
public class GroupActivityController {

    private final GroupActivityService groupActivityService;
    private final SchoolAuthService schoolAuthService;
    private final SchoolService schoolService;

    @PostMapping("/complete")
    public ResponseEntity<?> complete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody GroupActivityCompleteRequest request) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        SchoolAuthService.SchoolAuthContext ctx = auth.get();
        UUID schoolId = parseUuid(request.getSchoolId());
        UUID classId = parseUuid(request.getClassId());
        if (schoolId == null || classId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", "Invalid schoolId or classId"));
        }
        if (!ctx.schoolId().equals(schoolId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "School does not match your auth"));
        }
        if (!schoolService.canAccessClass(schoolId, classId, ctx.isTeacher() ? ctx.teacherId() : null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Class not found or access denied"));
        }

        GroupActivityCompleteResponse response = groupActivityService.complete(
                request, schoolId, classId, ctx.adminOrTeacherUserId(), ctx.teacherId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String schoolId,
            @RequestParam String classId) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        UUID schoolUuid = parseUuid(schoolId);
        UUID classUuid = parseUuid(classId);
        if (schoolUuid == null || classUuid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", "Invalid schoolId or classId"));
        }
        if (!auth.get().schoolId().equals(schoolUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "School does not match your auth"));
        }
        if (!schoolService.canAccessClass(schoolUuid, classUuid, auth.get().isTeacher() ? auth.get().teacherId() : null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Class not found or access denied"));
        }

        GroupActivityHistoryResponse response = groupActivityService.getHistory(schoolUuid, classUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String schoolId,
            @RequestParam String classId) {

        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }

        UUID schoolUuid = parseUuid(schoolId);
        UUID classUuid = parseUuid(classId);
        if (schoolUuid == null || classUuid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", "Invalid schoolId or classId"));
        }
        if (!auth.get().schoolId().equals(schoolUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "School does not match your auth"));
        }
        if (!schoolService.canAccessClass(schoolUuid, classUuid, auth.get().isTeacher() ? auth.get().teacherId() : null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Class not found or access denied"));
        }

        GroupActivityLeaderboardResponse response = groupActivityService.getLeaderboard(schoolUuid, classUuid);
        return ResponseEntity.ok(response);
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
