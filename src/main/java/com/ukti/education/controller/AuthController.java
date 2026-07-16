package com.ukti.education.controller;

import com.ukti.education.dto.TeacherLoginRequest;
import com.ukti.education.dto.TeacherLoginResponse;
import com.ukti.education.dto.TeacherResponse;
import com.ukti.education.service.SuperAdminService;
import com.ukti.education.service.TeacherJwtService;
import com.ukti.education.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final TeacherService teacherService;
    private final TeacherJwtService teacherJwtService;
    private final SuperAdminService superAdminService;

    @PostMapping("/teacher/login")
    public ResponseEntity<?> teacherLogin(@Valid @RequestBody TeacherLoginRequest request) {
        UUID schoolId = null;
        if (request.getSchoolId() != null && !request.getSchoolId().isBlank()) {
            try {
                schoolId = UUID.fromString(request.getSchoolId().trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Optional<TeacherResponse> teacherOpt = teacherService.validateAndGetTeacher(
                request.getEmail(), request.getPassword(), schoolId);

        if (teacherOpt.isEmpty()) {
            log.warn("Teacher login failed for email {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("LOGIN_FAILED", "Teacher login failed"));
        }

        TeacherResponse teacher = teacherOpt.get();
        String token = teacherJwtService.createToken(
                UUID.fromString(teacher.getId()),
                UUID.fromString(teacher.getSchoolId()));

        TeacherLoginResponse response = TeacherLoginResponse.builder()
                .token(token)
                .teacher(teacher)
                .build();

        log.info("Teacher login success: {}", teacher.getEmail());
        return ResponseEntity.ok(response);
    }

    /** DB email/password super admin — no Cognito. */
    @PostMapping("/super-admin/login")
    public ResponseEntity<?> superAdminLogin(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String password = body != null ? body.get("password") : null;
        Optional<Map<String, Object>> result = superAdminService.login(email, password);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("LOGIN_FAILED", "Super admin login failed"));
        }
        log.info("Super admin login success: {}", email);
        return ResponseEntity.ok(result.get());
    }
}
