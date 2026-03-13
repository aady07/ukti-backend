package com.ukti.education.service;

import com.ukti.education.entity.User;
import com.ukti.education.repository.TeacherClassRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the current user from Cognito JWT or X-Cognito-Sub header.
 * When X-Roll-Number is present and caller is school_admin, resolves to the student's user ID.
 */
@Service
@RequiredArgsConstructor
public class AuthContextService {

    private static final String USER_TYPE_SCHOOL_ADMIN = "school_admin";

    private final CognitoJwtService cognitoJwtService;
    private final TeacherJwtService teacherJwtService;
    private final UserRepository userRepository;
    private final TeacherClassRepository teacherClassRepository;

    /**
     * Returns the effective user ID for progress APIs.
     * Accepts both Cognito JWT (admin) and teacher JWT.
     * If X-Roll-Number present: resolves to student's user ID (teacher or admin).
     * If teacher token + no X-Roll-Number: returns empty (teacher has no progress; return empty data).
     * If Cognito + no X-Roll-Number: returns admin's user ID.
     */
    public Optional<UUID> resolveEffectiveUserId(
            String authorization,
            String cognitoSubHeader,
            String rollNumberHeader,
            String classIdHeader) {

        if (rollNumberHeader != null && !rollNumberHeader.isBlank()) {
            return resolveStudentForRollNumber(authorization, cognitoSubHeader, rollNumberHeader.trim(), classIdHeader);
        }

        // No roll number: try teacher token first (teacher has no user progress → treat as "empty" case)
        if (teacherJwtService.validateAndExtract(authorization).isPresent()) {
            return Optional.empty();  // Signal: authorized teacher, no student selected → return empty progress
        }

        return resolveUserId(authorization, cognitoSubHeader);
    }

    /**
     * Returns true if the request is authorized (teacher or Cognito).
     * Used when we need to allow teacher without roll number to get empty progress.
     */
    public boolean isAuthorized(String authorization, String cognitoSubHeader) {
        if (teacherJwtService.validateAndExtract(authorization).isPresent()) return true;
        return resolveUserId(authorization, cognitoSubHeader).isPresent();
    }

    /**
     * Resolves student when school_admin or teacher passes X-Roll-Number.
     * Returns empty if not authorized or student not found.
     */
    private Optional<UUID> resolveStudentForRollNumber(
            String authorization,
            String cognitoSubHeader,
            String rollNumber,
            String classIdHeader) {

        UUID schoolId = null;

        // Try teacher token first
        Optional<TeacherJwtService.TeacherTokenClaims> teacherClaims = teacherJwtService.validateAndExtract(authorization);
        if (teacherClaims.isPresent()) {
            schoolId = teacherClaims.get().schoolId();
        }

        // Try Cognito school_admin
        if (schoolId == null) {
            Optional<User> authUser = userRepository.findByCognitoSub(resolveCognitoSub(authorization, cognitoSubHeader));
            if (authUser.isEmpty()) return Optional.empty();
            User user = authUser.get();
            if (!USER_TYPE_SCHOOL_ADMIN.equals(user.getUserType()) || user.getSchoolUuid() == null) {
                return Optional.empty();
            }
            schoolId = user.getSchoolUuid();
        }

        if (schoolId == null) return Optional.empty();
        UUID classId = null;
        if (classIdHeader != null && !classIdHeader.isBlank()) {
            try {
                classId = UUID.fromString(classIdHeader.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Optional<UUID> studentUserId;
        UUID studentClassId;
        if (classId != null) {
            Optional<User> student = userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(
                    schoolId, classId, rollNumber, "student");
            studentUserId = student.map(User::getId);
            studentClassId = student.map(User::getClassId).orElse(null);
        } else {
            var students = userRepository.findBySchoolUuidAndRollNumberAndUserType(
                    schoolId, rollNumber, "student");
            studentUserId = students.size() == 1 ? Optional.of(students.get(0).getId()) : Optional.empty();
            studentClassId = students.size() == 1 ? students.get(0).getClassId() : null;
        }

        // Teacher: only allow progress for students in classes assigned to that teacher
        if (teacherClaims.isPresent() && studentUserId.isPresent() && studentClassId != null) {
            UUID teacherId = teacherClaims.get().teacherId();
            if (!teacherClassRepository.existsByTeacherIdAndClassId(teacherId, studentClassId)) {
                return Optional.empty();  // Student not in teacher's assigned classes
            }
        }

        return studentUserId;
    }

    private String resolveCognitoSub(String authorization, String cognitoSubHeader) {
        if (authorization != null && !authorization.isBlank()) {
            Optional<String> sub = cognitoJwtService.validateAndExtract(authorization).map(c -> c.getSub());
            if (sub.isPresent()) return sub.get();
        }
        if (cognitoSubHeader != null && !cognitoSubHeader.isBlank()) {
            return cognitoSubHeader.trim();
        }
        return null;
    }

    /**
     * Returns the user ID (UUID) if the user exists in our DB.
     * Returns empty if JWT is invalid or user not found.
     */
    public Optional<UUID> resolveUserId(String authorization, String cognitoSubHeader) {
        String cognitoSub = resolveCognitoSub(authorization, cognitoSubHeader);
        if (cognitoSub == null || cognitoSub.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByCognitoSub(cognitoSub).map(User::getId);
    }
}
