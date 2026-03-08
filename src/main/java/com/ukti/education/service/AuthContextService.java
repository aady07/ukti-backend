package com.ukti.education.service;

import com.ukti.education.entity.User;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the current user from Cognito JWT or X-Cognito-Sub header.
 */
@Service
@RequiredArgsConstructor
public class AuthContextService {

    private final CognitoJwtService cognitoJwtService;
    private final UserRepository userRepository;

    /**
     * Returns the user ID (UUID) if the user exists in our DB.
     * Returns empty if JWT is invalid or user not found.
     */
    public Optional<UUID> resolveUserId(String authorization, String cognitoSubHeader) {
        String cognitoSub = null;

        if (authorization != null && !authorization.isBlank()) {
            cognitoSub = cognitoJwtService.validateAndExtract(authorization)
                    .map(c -> c.getSub())
                    .orElse(null);
        }
        if (cognitoSub == null && cognitoSubHeader != null && !cognitoSubHeader.isBlank()) {
            cognitoSub = cognitoSubHeader.trim();
        }

        if (cognitoSub == null || cognitoSub.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByCognitoSub(cognitoSub).map(User::getId);
    }
}
