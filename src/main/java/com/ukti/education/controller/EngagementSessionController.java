package com.ukti.education.controller;

import com.ukti.education.dto.EngagementSessionBatchRequest;
import com.ukti.education.dto.EngagementSessionBatchResponse;
import com.ukti.education.dto.EngagementSubject;
import com.ukti.education.service.ActivityEngagementService;
import com.ukti.education.service.AuthContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/engagement")
@RequiredArgsConstructor
@Slf4j
public class EngagementSessionController {

    private final ActivityEngagementService engagementService;
    private final AuthContextService authContextService;

    /**
     * Batch engagement analytics for one activity visit. Auth/headers align with {@code /progress/**}
     * (Cognito or teacher JWT; {@code X-Roll-Number} / {@code X-Class-Id} for station flows).
     * <p>
     * Merge rule: {@code counters} are <strong>deltas</strong> applied to running totals (see service javadoc).
     */
    @PostMapping("/sessions/batch")
    public ResponseEntity<?> postBatch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @RequestHeader(value = "X-Roll-Number", required = false) String rollNumberHeader,
            @RequestHeader(value = "X-Class-Id", required = false) String classIdHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody EngagementSessionBatchRequest request) {

        log.info("engagement_batch clientSessionId={} batchId={} unit={} activity={}",
                request.getClientSessionId(), request.getBatchId(), request.getUnitSlug(), request.getActivitySlug());

        Optional<EngagementSubject> subject = authContextService.resolveEngagementSubject(
                authorization, cognitoSubHeader, rollNumberHeader, classIdHeader);
        if (subject.isEmpty()) {
            if (rollNumberHeader != null && !rollNumberHeader.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new UserController.ErrorResponse("STUDENT_NOT_FOUND", "Student not found for roll number"));
            }
            if (authContextService.isAuthorized(authorization, cognitoSubHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new UserController.ErrorResponse("STUDENT_CONTEXT_REQUIRED",
                                "Select a student (X-Roll-Number) or sign in as a learner to record engagement"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito or teacher JWT required"));
        }

        try {
            EngagementSessionBatchResponse body = engagementService.ingestBatch(subject.get(), request, idempotencyKey);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("engagement_batch_bad_request {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("engagement_batch_forbidden {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (Exception e) {
            log.error("engagement_batch_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to record engagement"));
        }
    }
}
