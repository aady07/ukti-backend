package com.ukti.education.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ukti.education.dto.ExperientialStatRequest;
import com.ukti.education.dto.ExperientialStatResponse;
import com.ukti.education.dto.ModuleProgressResponse;
import com.ukti.education.dto.TaskCompletionRequest;
import com.ukti.education.dto.TaskCompletionResponse;
import com.ukti.education.dto.UserProgressResponse;
import com.ukti.education.service.AuthContextService;
import com.ukti.education.service.ProgressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "https://miraista.com", "http://miraista.com", "https://education.miraista.com", "http://education.miraista.com", "https://ukti.example.com"})
public class ProgressController {

    private final ProgressService progressService;
    private final AuthContextService authContextService;

    @PostMapping("/module/{moduleId}/complete")
    public ResponseEntity<?> completeModule(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @PathVariable String moduleId) {

        log.info("API POST /progress/module/{}/complete called", moduleId);

        Optional<UUID> userId = authContextService.resolveUserId(authorization, cognitoSubHeader);
        if (userId.isEmpty()) {
            log.warn("API POST /progress/module/{}/complete FAILED: 401 Unauthorized", moduleId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT required"));
        }

        try {
            ModuleProgressResponse response = progressService.completeModule(userId.get(), moduleId);
            log.info("API POST /progress/module/{}/complete SUCCESS: 201", moduleId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("API POST /progress/module/{}/complete FAILED: 500 - {}", moduleId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/module/{moduleId}")
    public ResponseEntity<?> getModuleProgress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @PathVariable String moduleId) {

        log.info("API GET /progress/module/{} called", moduleId);

        Optional<UUID> userId = authContextService.resolveUserId(authorization, cognitoSubHeader);
        if (userId.isEmpty()) {
            log.warn("API GET /progress/module/{} FAILED: 401 Unauthorized", moduleId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT required"));
        }

        Optional<ModuleProgressResponse> response = progressService.getModuleProgress(userId.get(), moduleId);
        if (response.isPresent()) {
            log.info("API GET /progress/module/{} SUCCESS: 200", moduleId);
            return ResponseEntity.ok(response.get());
        }
        log.warn("API GET /progress/module/{} FAILED: 404", moduleId);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserProgress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader) {

        log.info("API GET /progress/user called");

        Optional<UUID> userId = authContextService.resolveUserId(authorization, cognitoSubHeader);
        if (userId.isEmpty()) {
            log.warn("API GET /progress/user FAILED: 401 Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT required"));
        }

        UserProgressResponse response = progressService.getUserProgress(userId.get());
        log.info("API GET /progress/user SUCCESS: 200");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/task")
    public ResponseEntity<?> recordTaskCompletion(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @Valid @RequestBody TaskCompletionRequest request) {

        log.info("API POST /progress/task called, moduleId={}, taskId={}", request.getModuleId(), request.getTaskId());

        Optional<UUID> userId = authContextService.resolveUserId(authorization, cognitoSubHeader);
        if (userId.isEmpty()) {
            log.warn("API POST /progress/task FAILED: 401 Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT required"));
        }

        try {
            TaskCompletionResponse response = progressService.recordTaskCompletion(userId.get(), request);
            log.info("API POST /progress/task SUCCESS: 201, taskId={}", response.getTaskId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("API POST /progress/task FAILED: 500 - {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/experiential/{moduleId}")
    public ResponseEntity<?> updateExperientialStat(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader,
            @PathVariable String moduleId,
            @Valid @RequestBody ExperientialStatRequest request) {

        log.info("API PUT /progress/experiential/{} called, statType={}", moduleId, request.getStatType());

        Optional<UUID> userId = authContextService.resolveUserId(authorization, cognitoSubHeader);
        if (userId.isEmpty()) {
            log.warn("API PUT /progress/experiential/{} FAILED: 401 Unauthorized", moduleId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT required"));
        }

        try {
            ExperientialStatResponse response = progressService.updateExperientialStat(userId.get(), moduleId, request);
            log.info("API PUT /progress/experiential/{} SUCCESS: 200, statType={}, count={}", moduleId, response.getStatType(), response.getCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("API PUT /progress/experiential/{} FAILED: 500 - {}", moduleId, e.getMessage(), e);
            throw e;
        }
    }
}
