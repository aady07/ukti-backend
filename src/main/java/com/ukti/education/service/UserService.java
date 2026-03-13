package com.ukti.education.service;

import com.ukti.education.dto.UserRequest;
import com.ukti.education.dto.UserResponse;
import com.ukti.education.entity.School;
import com.ukti.education.entity.User;
import com.ukti.education.repository.SchoolRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String SIGNUP_TYPE_ORGANIZATION = "organization";
    private static final String USER_TYPE_INDIVIDUAL = "individual";
    private static final String USER_TYPE_SCHOOL_ADMIN = "school_admin";

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    @Transactional
    public UserResponse createOrUpdateUser(UserRequest request) {
        boolean isOrganizationSignup = SIGNUP_TYPE_ORGANIZATION.equalsIgnoreCase(request.getSignupType())
                && request.getOrganizationName() != null && !request.getOrganizationName().isBlank();

        if (isOrganizationSignup) {
            return createSchoolAdmin(request);
        }

        return createOrUpdateIndividualUser(request);
    }

    @Transactional
    protected UserResponse createSchoolAdmin(UserRequest request) {
        Optional<User> existing = userRepository.findByCognitoSub(request.getCognitoSub());
        if (existing.isPresent()) {
            log.info("UserService: Updating existing school admin cognitoSub={}", request.getCognitoSub());
            User user = existing.get();
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setUsername(request.getUsername());
            if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
            user = userRepository.save(user);
            return toResponse(user);
        }

        School school = School.builder()
                .name(request.getOrganizationName().trim())
                .build();
        school = schoolRepository.save(school);

        User user = User.builder()
                .cognitoSub(request.getCognitoSub())
                .email(request.getEmail())
                .phone(request.getPhone())
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .schoolUuid(school.getId())
                .userType(USER_TYPE_SCHOOL_ADMIN)
                .passwordHash("COGNITO")
                .build();
        user = userRepository.save(user);
        log.info("UserService: Created school admin and school cognitoSub={}, schoolId={}", request.getCognitoSub(), school.getId());
        return toResponse(user);
    }

    @Transactional
    protected UserResponse createOrUpdateIndividualUser(UserRequest request) {
        Optional<User> existing = userRepository.findByCognitoSub(request.getCognitoSub());

        User user;
        if (existing.isPresent()) {
            log.info("UserService: Updating existing user cognitoSub={}", request.getCognitoSub());
            user = existing.get();
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setUsername(request.getUsername());
            if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
            if (request.getSchoolId() != null) user.setSchoolId(request.getSchoolId());
            if (user.getUserType() == null) user.setUserType(USER_TYPE_INDIVIDUAL);
        } else {
            log.info("UserService: Creating new user cognitoSub={}, email={}", request.getCognitoSub(), request.getEmail());
            user = User.builder()
                    .cognitoSub(request.getCognitoSub())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .username(request.getUsername())
                    .displayName(request.getDisplayName())
                    .schoolId(request.getSchoolId())
                    .userType(USER_TYPE_INDIVIDUAL)
                    .passwordHash("COGNITO")
                    .build();
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    public Optional<UserResponse> getUserById(UUID id) {
        return userRepository.findById(id).map(this::toResponse);
    }

    public Optional<UserResponse> getUserByCognitoSub(String cognitoSub) {
        return userRepository.findByCognitoSub(cognitoSub).map(this::toResponse);
    }

    public Optional<User> getUserEntityByCognitoSub(String cognitoSub) {
        return userRepository.findByCognitoSub(cognitoSub);
    }

    public Optional<User> getUserEntityById(UUID id) {
        return userRepository.findById(id);
    }

    private UserResponse toResponse(User user) {
        String schoolName = user.getSchoolUuid() != null
                ? schoolRepository.findById(user.getSchoolUuid()).map(s -> s.getName()).orElse(null)
                : null;
        return UserResponse.builder()
                .id(user.getId().toString())
                .cognitoSub(user.getCognitoSub())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .schoolId(user.getSchoolId())
                .userType(user.getUserType() != null ? user.getUserType() : USER_TYPE_INDIVIDUAL)
                .schoolUuid(user.getSchoolUuid() != null ? user.getSchoolUuid().toString() : null)
                .schoolName(schoolName)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
