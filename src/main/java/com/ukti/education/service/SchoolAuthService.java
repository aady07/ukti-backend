package com.ukti.education.service;

import com.ukti.education.dto.CognitoUserClaims;
import com.ukti.education.entity.User;
import com.ukti.education.service.TeacherJwtService.TeacherTokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves school auth: either Cognito school_admin or teacher token.
 */
@Service
@RequiredArgsConstructor
public class SchoolAuthService {

    private static final String USER_TYPE_SCHOOL_ADMIN = "school_admin";

    private final CognitoJwtService cognitoJwtService;
    private final TeacherJwtService teacherJwtService;
    private final UserService userService;

    public static record SchoolAuthContext(UUID schoolId, UUID adminOrTeacherUserId, UUID teacherId, boolean isTeacher) {}

    /**
     * Returns auth context if caller is school_admin (Cognito) or teacher (teacher token).
     * adminOrTeacherUserId: for admin = user.id, for teacher = null.
     * teacherId: for teacher = teacher id, for admin = null.
     */
    public Optional<SchoolAuthContext> resolveSchoolAuth(String authorization) {
        // Try teacher token first
        Optional<TeacherTokenClaims> teacherClaims = teacherJwtService.validateAndExtract(authorization);
        if (teacherClaims.isPresent()) {
            return Optional.of(new SchoolAuthContext(
                    teacherClaims.get().schoolId(),
                    null,
                    teacherClaims.get().teacherId(),
                    true));
        }

        // Try Cognito school_admin
        Optional<CognitoUserClaims> claims = cognitoJwtService.validateAndExtract(authorization);
        if (claims.isEmpty()) return Optional.empty();

        Optional<User> user = userService.getUserEntityByCognitoSub(claims.get().getSub());
        if (user.isEmpty()) return Optional.empty();
        if (!USER_TYPE_SCHOOL_ADMIN.equals(user.get().getUserType()) || user.get().getSchoolUuid() == null) {
            return Optional.empty();
        }

        return Optional.of(new SchoolAuthContext(
                user.get().getSchoolUuid(),
                user.get().getId(),
                null,
                false));
    }
}
