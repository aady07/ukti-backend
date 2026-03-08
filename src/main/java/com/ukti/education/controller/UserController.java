package com.ukti.education.controller;

import com.ukti.education.dto.CognitoUserClaims;
import com.ukti.education.dto.UserRequest;
import com.ukti.education.dto.UserResponse;
import com.ukti.education.service.CognitoJwtService;
import com.ukti.education.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5172", "https://miraista.com", "http://miraista.com", "https://www.miraista.com", "http://www.miraista.com", "https://education.miraista.com", "http://education.miraista.com", "https://educationuat.miraista.com", "http://educationuat.miraista.com", "https://ukti.example.com"})
public class UserController {

    private final UserService userService;
    private final CognitoJwtService cognitoJwtService;

    /**
     * Create or update user after Cognito signup/login.
     * Requires: Authorization: Bearer &lt;cognito_jwt&gt;
     * Body is optional - user info is extracted from the JWT. Pass displayName, schoolId to override.
     */
    @PostMapping
    public ResponseEntity<?> createOrUpdateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) UserRequest body) {

        log.info("API POST /users called, hasAuth={}", authorization != null && !authorization.isBlank());

        Optional<CognitoUserClaims> claims = cognitoJwtService.validateAndExtract(authorization);
        if (claims.isEmpty()) {
            log.warn("API POST /users FAILED: 401 Unauthorized - missing or invalid Cognito JWT");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT required in Authorization header"));
        }

        CognitoUserClaims c = claims.get();
        UserRequest request = body != null ? body : new UserRequest();
        request.setCognitoSub(c.getSub());
        String email = (c.getEmail() != null && !c.getEmail().isBlank()) ? c.getEmail() : (c.getSub() + "@cognito.local");
        request.setEmail(email);
        request.setPhone(c.getPhone());
        request.setUsername(c.getUsername() != null ? c.getUsername() : c.getSub());
        if (body != null) {
            if (body.getDisplayName() != null) request.setDisplayName(body.getDisplayName());
            if (body.getSchoolId() != null) request.setSchoolId(body.getSchoolId());
        }

        try {
            UserResponse response = userService.createOrUpdateUser(request);
            log.info("API POST /users SUCCESS: 201 Created, cognitoSub={}, userId={}", c.getSub(), response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("API POST /users FAILED: 500 - cognitoSub={}, error={}", c.getSub(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get current user. Requires: Authorization: Bearer &lt;cognito_jwt&gt;
     * Or: X-Cognito-Sub header (legacy, for dev).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSubHeader) {

        log.info("API GET /users/me called, hasAuth={}, hasX-Cognito-Sub={}",
                authorization != null && !authorization.isBlank(),
                cognitoSubHeader != null && !cognitoSubHeader.isBlank());

        String cognitoSub = null;

        if (authorization != null && !authorization.isBlank()) {
            Optional<CognitoUserClaims> claims = cognitoJwtService.validateAndExtract(authorization);
            if (claims.isPresent()) {
                cognitoSub = claims.get().getSub();
            }
        }
        if (cognitoSub == null && cognitoSubHeader != null && !cognitoSubHeader.isBlank()) {
            cognitoSub = cognitoSubHeader.trim();
        }

        if (cognitoSub == null || cognitoSub.isBlank()) {
            log.warn("API GET /users/me FAILED: 401 Unauthorized - missing or invalid Cognito JWT / X-Cognito-Sub");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Valid Cognito JWT or X-Cognito-Sub header required"));
        }

        Optional<UserResponse> user = userService.getUserByCognitoSub(cognitoSub);
        if (user.isPresent()) {
            log.info("API GET /users/me SUCCESS: 200 OK, cognitoSub={}, userId={}", cognitoSub, user.get().getId());
            return ResponseEntity.ok(user.get());
        }
        log.warn("API GET /users/me FAILED: 404 Not Found - cognitoSub={}, user not in DB", cognitoSub);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "User not found. Call POST /users first."));
    }

    /**
     * Get user by ID (no auth required for now).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        log.info("API GET /users/{} called", id);

        Optional<UserResponse> user = userService.getUserById(id);
        if (user.isPresent()) {
            log.info("API GET /users/{} SUCCESS: 200 OK", id);
            return ResponseEntity.ok(user.get());
        }
        log.warn("API GET /users/{} FAILED: 404 Not Found", id);
        return ResponseEntity.notFound().build();
    }

    public record ErrorResponse(String error, String message) {}
}
