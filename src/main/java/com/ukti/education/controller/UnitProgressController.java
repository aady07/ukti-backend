package com.ukti.education.controller;

import com.ukti.education.dto.*;
import com.ukti.education.service.AuthContextService;
import com.ukti.education.service.UserActivityProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/progress/units")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5172", "https://miraista.com", "http://miraista.com", "https://www.miraista.com", "http://www.miraista.com", "https://education.miraista.com", "http://education.miraista.com", "https://educationuat.miraista.com", "http://educationuat.miraista.com", "https://ukti.example.com"})
public class UnitProgressController {

    private final UserActivityProgressService progressService;
    private final AuthContextService authContextService;

    @GetMapping
    public ResponseEntity<?> getAllProgress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @RequestHeader(value = "X-Roll-Number", required = false) String rollNumberHeader,
            @RequestHeader(value = "X-Class-Id", required = false) String classIdHeader) {

        log.info("API GET /progress/units called");

        Optional<UUID> userId = authContextService.resolveEffectiveUserId(authorization, cognitoSubHeader, rollNumberHeader, classIdHeader);
        if (userId.isEmpty()) {
            if (rollNumberHeader != null && !rollNumberHeader.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new UserController.ErrorResponse("STUDENT_NOT_FOUND", "Student not found for roll number"));
            }
            if (authContextService.isAuthorized(authorization, cognitoSubHeader)) {
                return ResponseEntity.ok(List.of());
            }
            log.warn("API GET /progress/units FAILED: 401 Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito or teacher JWT required"));
        }

        List<UnitProgressResponse> response = progressService.getAllProgress(userId.get());
        log.info("API GET /progress/units SUCCESS: 200, count={}", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{unitSlug}")
    public ResponseEntity<?> getUnitProgress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @RequestHeader(value = "X-Roll-Number", required = false) String rollNumberHeader,
            @RequestHeader(value = "X-Class-Id", required = false) String classIdHeader,
            @PathVariable String unitSlug) {

        log.info("API GET /progress/units/{} called", unitSlug);

        Optional<UUID> userId = authContextService.resolveEffectiveUserId(authorization, cognitoSubHeader, rollNumberHeader, classIdHeader);
        if (userId.isEmpty()) {
            if (rollNumberHeader != null && !rollNumberHeader.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new UserController.ErrorResponse("STUDENT_NOT_FOUND", "Student not found for roll number"));
            }
            if (authContextService.isAuthorized(authorization, cognitoSubHeader)) {
                return ResponseEntity.ok(UnitProgressResponse.builder()
                        .unitId(unitSlug)
                        .completedActivities(List.of())
                        .completedCount(0)
                        .build());
            }
            log.warn("API GET /progress/units/{} FAILED: 401 Unauthorized", unitSlug);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito or teacher JWT required"));
        }

        Optional<UnitProgressResponse> response = progressService.getUnitProgress(userId.get(), unitSlug);
        if (response.isPresent()) {
            log.info("API GET /progress/units/{} SUCCESS: 200", unitSlug);
            return ResponseEntity.ok(response.get());
        }
        return ResponseEntity.ok(UnitProgressResponse.builder()
                .unitId(unitSlug)
                .completedActivities(List.of())
                .completedCount(0)
                .build());
    }

    @PostMapping("/{unitSlug}/activities/{activitySlug}/complete")
    public ResponseEntity<?> completeActivity(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @RequestHeader(value = "X-Roll-Number", required = false) String rollNumberHeader,
            @RequestHeader(value = "X-Class-Id", required = false) String classIdHeader,
            @PathVariable String unitSlug,
            @PathVariable String activitySlug,
            @RequestBody(required = false) ActivityCompleteRequest request) {

        log.info("API POST /progress/units/{}/activities/{}/complete called", unitSlug, activitySlug);

        Optional<UUID> userId = authContextService.resolveEffectiveUserId(authorization, cognitoSubHeader, rollNumberHeader, classIdHeader);
        if (userId.isEmpty()) {
            if (rollNumberHeader != null && !rollNumberHeader.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new UserController.ErrorResponse("STUDENT_NOT_FOUND", "Student not found for roll number"));
            }
            log.warn("API POST /progress/units/{}/activities/{}/complete FAILED: 401 Unauthorized", unitSlug, activitySlug);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito or teacher JWT required. For teachers, X-Roll-Number is required."));
        }

        Map<String, Object> metadata = request != null ? request.getMetadata() : null;
        Optional<ActivityCompleteResponse> response = progressService.completeActivity(
                userId.get(), unitSlug, activitySlug, metadata);

        if (response.isPresent()) {
            log.info("API POST /progress/units/{}/activities/{}/complete SUCCESS: 201", unitSlug, activitySlug);
            return ResponseEntity.status(HttpStatus.CREATED).body(response.get());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
