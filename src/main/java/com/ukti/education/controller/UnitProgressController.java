package com.ukti.education.controller;

import com.ukti.education.dto.*;
import com.ukti.education.entity.User;
import com.ukti.education.repository.UserRepository;
import com.ukti.education.service.ActivityMediaService;
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
public class UnitProgressController {

    private final UserActivityProgressService progressService;
    private final AuthContextService authContextService;
    private final ActivityMediaService activityMediaService;
    private final UserRepository userRepository;

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

    /**
     * Presign S3 PUT URLs so the browser uploads image/audio directly (same API auth as complete).
     * Store returned keys in {@link ActivityMediaService#META_IMAGE_KEY} / {@link ActivityMediaService#META_AUDIO_KEY}
     * via the existing complete call metadata.
     */
    @PostMapping("/{unitSlug}/activities/{activitySlug}/media/presign")
    public ResponseEntity<?> presignActivityMedia(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @RequestHeader(value = "X-Roll-Number", required = false) String rollNumberHeader,
            @RequestHeader(value = "X-Class-Id", required = false) String classIdHeader,
            @PathVariable String unitSlug,
            @PathVariable String activitySlug,
            @RequestBody(required = false) MediaPresignRequest request) {

        Optional<UUID> userId = authContextService.resolveEffectiveUserId(authorization, cognitoSubHeader, rollNumberHeader, classIdHeader);
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito or teacher JWT required"));
        }
        Optional<User> userOpt = userRepository.findById(userId.get());
        if (userOpt.isEmpty() || userOpt.get().getSchoolUuid() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("MEDIA_UNAVAILABLE", "Activity media requires a school-scoped student account"));
        }
        try {
            MediaPresignResponse body = activityMediaService.presignUpload(
                    userId.get(),
                    userOpt.get().getSchoolUuid(),
                    unitSlug,
                    activitySlug,
                    request != null ? request.getImageContentType() : null,
                    request != null ? request.getAudioContentType() : null);
            return ResponseEntity.ok(body);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new UserController.ErrorResponse("MEDIA_DISABLED", e.getReason() != null ? e.getReason() : "Media storage off"));
            }
            throw e;
        }
    }
}
