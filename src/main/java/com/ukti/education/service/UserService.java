package com.ukti.education.service;

import com.ukti.education.dto.UserRequest;
import com.ukti.education.dto.UserResponse;
import com.ukti.education.entity.User;
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

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createOrUpdateUser(UserRequest request) {
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
        } else {
            log.info("UserService: Creating new user cognitoSub={}, email={}", request.getCognitoSub(), request.getEmail());
            user = User.builder()
                    .cognitoSub(request.getCognitoSub())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .username(request.getUsername())
                    .displayName(request.getDisplayName())
                    .schoolId(request.getSchoolId())
                    .passwordHash("COGNITO")  // Placeholder - Cognito handles auth; satisfies DB NOT NULL
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

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .cognitoSub(user.getCognitoSub())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .schoolId(user.getSchoolId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
