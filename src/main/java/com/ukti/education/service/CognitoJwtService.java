package com.ukti.education.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.ukti.education.dto.CognitoUserClaims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Optional;

/**
 * Validates Cognito JWT tokens and extracts user claims.
 * JWKS URL: https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json
 */
@Service
@Slf4j
public class CognitoJwtService {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public CognitoJwtService(
            @Value("${ukti.cognito.jwks-url:https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_XYqySdLwI/.well-known/jwks.json}") String jwksUrl) {
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUrl));
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            this.jwtProcessor = new DefaultJWTProcessor<>();
            this.jwtProcessor.setJWSKeySelector(keySelector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Cognito JWKS: " + e.getMessage());
        }
    }

    /**
     * Validates the Cognito JWT and returns user claims if valid.
     */
    public Optional<CognitoUserClaims> validateAndExtract(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : bearerToken.trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }

        try {
            JWTClaimsSet claims = jwtProcessor.process(token, null);

            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                return Optional.empty();
            }

            String email = getStringClaim(claims, "email");
            String phone = getStringClaim(claims, "phone_number");
            String username = getStringClaim(claims, "cognito:username");
            if (username == null) {
                username = getStringClaim(claims, "username");
            }
            if (username == null) {
                username = email != null ? email : sub;
            }

            return Optional.of(CognitoUserClaims.builder()
                    .sub(sub)
                    .email(email != null ? email : "")
                    .phone(phone)
                    .username(username)
                    .build());
        } catch (Exception e) {
            log.warn("Cognito JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String getStringClaim(JWTClaimsSet claims, String name) {
        Object val = claims.getClaim(name);
        return val != null ? val.toString() : null;
    }
}
