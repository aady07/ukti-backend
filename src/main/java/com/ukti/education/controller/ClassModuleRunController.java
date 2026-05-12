package com.ukti.education.controller;

import com.ukti.education.dto.*;
import com.ukti.education.service.ClassModuleRunException;
import com.ukti.education.service.ClassModuleRunService;
import com.ukti.education.service.SchoolAuthService;
import com.ukti.education.service.SchoolService;
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
@RequestMapping("/v1/class-modules/runs")
@RequiredArgsConstructor
@Slf4j
public class ClassModuleRunController {

    private final ClassModuleRunService classModuleRunService;
    private final SchoolAuthService schoolAuthService;
    private final SchoolService schoolService;

    @PostMapping("/start")
    public ResponseEntity<?> startRun(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ClassModuleRunStartRequest request) {
        log.info("class_run_start_request schoolId={} classId={} moduleId={} sectionCount={}",
                request.getSchoolId(), request.getClassId(), request.getModuleId(),
                request.getSectionIds() != null ? request.getSectionIds().size() : 0);
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty() || !auth.get().schoolId().equals(request.getSchoolId())) {
            log.warn("class_run_start_denied reason=UNAUTHORIZED schoolId={} classId={} moduleId={}",
                    request.getSchoolId(), request.getClassId(), request.getModuleId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        if (!schoolService.canAccessClass(request.getSchoolId(), request.getClassId(), auth.get().isTeacher() ? auth.get().teacherId() : null)) {
            log.warn("class_run_start_denied reason=FORBIDDEN schoolId={} classId={} moduleId={} teacherId={}",
                    request.getSchoolId(), request.getClassId(), request.getModuleId(), auth.get().teacherId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", "Class not found or access denied"));
        }
        try {
            ClassModuleRunStartResponse response = classModuleRunService.startOrResume(request, auth.get().adminOrTeacherUserId());
            log.info("class_run_start_success runId={} schoolId={} classId={} moduleId={}",
                    response.getRunId(), request.getSchoolId(), request.getClassId(), request.getModuleId());
            return ResponseEntity.ok(response);
        } catch (ClassModuleRunException e) {
            log.warn("class_run_start_error code={} schoolId={} classId={} moduleId={} message={}",
                    e.getCode(), request.getSchoolId(), request.getClassId(), request.getModuleId(), e.getMessage());
            return toErrorResponse(e);
        }
    }

    @PostMapping("/{runId}/rolls/{rollNumber}/resolve-next")
    public ResponseEntity<?> resolveNext(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID runId,
            @PathVariable String rollNumber) {
        log.info("roll_resolve_request runId={} rollNumber={}", runId, rollNumber);
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            log.warn("roll_resolve_denied reason=UNAUTHORIZED runId={} rollNumber={}", runId, rollNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        try {
            validateRunAccess(runId, auth.get());
            RollResolveNextResponse response = classModuleRunService.resolveNext(runId, rollNumber);
            log.info("roll_resolve_response runId={} rollNumber={} allowed={} reason={} sectionId={} activityId={}",
                    runId, rollNumber, response.isAllowed(), response.getReason(), response.getSectionId(), response.getActivityId());
            return ResponseEntity.ok(response);
        } catch (ClassModuleRunException e) {
            log.warn("roll_resolve_error runId={} rollNumber={} code={} message={}",
                    runId, rollNumber, e.getCode(), e.getMessage());
            return toErrorResponse(e);
        }
    }

    @PostMapping("/{runId}/rolls/{rollNumber}/activities/{activityId}/complete")
    public ResponseEntity<?> completeActivity(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable UUID runId,
            @PathVariable String rollNumber,
            @PathVariable String activityId,
            @Valid @RequestBody ClassModuleActivityCompleteRequest request) {
        log.info("activity_complete_request runId={} rollNumber={} sectionId={} activityId={} challengeIndex={} idempotencyKeyPresent={}",
                runId, rollNumber, request.getSectionId(), activityId, request.getChallengeIndex(),
                idempotencyKey != null && !idempotencyKey.isBlank());
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            log.warn("activity_complete_denied reason=UNAUTHORIZED runId={} rollNumber={}", runId, rollNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        try {
            validateRunAccess(runId, auth.get());
            ClassModuleActivityCompleteResponse response =
                    classModuleRunService.completeActivity(runId, rollNumber, activityId, idempotencyKey, request);
            log.info("activity_complete_response runId={} rollNumber={} nextAction={} studentSectionStatus={} classSectionStatus={}",
                    runId, rollNumber, response.getNextAction(), response.getStudentSectionStatus(), response.getClassSectionStatus());
            return ResponseEntity.ok(response);
        } catch (ClassModuleRunException e) {
            log.warn("activity_complete_error runId={} rollNumber={} code={} message={}",
                    runId, rollNumber, e.getCode(), e.getMessage());
            return toErrorResponse(e);
        }
    }

    @GetMapping("/{runId}/sections/{sectionId}/summary")
    public ResponseEntity<?> sectionSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID runId,
            @PathVariable String sectionId) {
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        try {
            validateRunAccess(runId, auth.get());
            return ResponseEntity.ok(classModuleRunService.sectionSummary(runId, sectionId));
        } catch (ClassModuleRunException e) {
            return toErrorResponse(e);
        }
    }

    @GetMapping("/{runId}/status")
    public ResponseEntity<?> runStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID runId) {
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        try {
            validateRunAccess(runId, auth.get());
            return ResponseEntity.ok(classModuleRunService.runStatus(runId));
        } catch (ClassModuleRunException e) {
            return toErrorResponse(e);
        }
    }

    @PatchMapping("/{runId}")
    public ResponseEntity<?> patchRun(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID runId,
            @RequestBody ClassModuleRunPatchRequest request) {
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        try {
            validateRunAccess(runId, auth.get());
            return ResponseEntity.ok(classModuleRunService.patchRun(runId, request));
        } catch (ClassModuleRunException e) {
            return toErrorResponse(e);
        }
    }

    @PatchMapping("/{runId}/runtime-state")
    public ResponseEntity<?> patchRuntimeState(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID runId,
            @RequestBody Map<String, Object> body) {
        Optional<SchoolAuthService.SchoolAuthContext> auth = schoolAuthService.resolveSchoolAuth(authorization);
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid school admin or teacher JWT required"));
        }
        try {
            validateRunAccess(runId, auth.get());
            return ResponseEntity.ok(classModuleRunService.patchRuntimeState(runId, body));
        } catch (ClassModuleRunException e) {
            return toErrorResponse(e);
        }
    }

    private void validateRunAccess(UUID runId, SchoolAuthService.SchoolAuthContext auth) {
        ClassModuleRunService.RunScope scope = classModuleRunService.getRunScope(runId);
        if (!auth.schoolId().equals(scope.schoolId())
                || !schoolService.canAccessClass(scope.schoolId(), scope.classId(), auth.isTeacher() ? auth.teacherId() : null)) {
            throw new ClassModuleRunException("FORBIDDEN", "Run not found or access denied");
        }
    }

    private ResponseEntity<UserController.ErrorResponse> toErrorResponse(ClassModuleRunException e) {
        HttpStatus status = switch (e.getCode()) {
            case "RUN_NOT_FOUND", "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "RUN_COMPLETED", "SECTION_LOCKED", "IDEMPOTENCY_CONFLICT", "CONCURRENT_UPDATE_RETRY" -> HttpStatus.CONFLICT;
            case "ROLL_NOT_IN_CLASS", "INVALID_SECTION_FOR_ROLL", "INVALID_ACTIVITY_FOR_SECTION", "BAD_REQUEST", "CURRICULUM_MISSING" ->
                    HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new UserController.ErrorResponse(e.getCode(), e.getMessage()));
    }
}
