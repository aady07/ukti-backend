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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Validates D2C Cognito JWTs (try customers + super_admin).
 * Falls back to school JWKS URL when d2c.jwks-url is empty (dev until pool is wired).
 */
@Service
@Slf4j
public class CognitoD2cJwtService {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final boolean configured;

    public CognitoD2cJwtService(
            @Value("${ukti.cognito.d2c.jwks-url:}") String d2cJwksUrl,
            @Value("${ukti.cognito.jwks-url:https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_XYqySdLwI/.well-known/jwks.json}") String schoolJwksUrl) {
        String url = (d2cJwksUrl != null && !d2cJwksUrl.isBlank()) ? d2cJwksUrl.trim() : schoolJwksUrl;
        this.configured = url != null && !url.isBlank();
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(url));
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            this.jwtProcessor = new DefaultJWTProcessor<>();
            this.jwtProcessor.setJWSKeySelector(keySelector);
            log.info("CognitoD2cJwtService initialized jwks={}", url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize D2C Cognito JWKS: " + e.getMessage());
        }
    }

    public Optional<CognitoUserClaims> validateAndExtract(String bearerToken) {
        if (!configured || bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String token = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7).trim()
                : bearerToken.trim();
        if (token.isEmpty()) return Optional.empty();

        try {
            JWTClaimsSet claims = jwtProcessor.process(token, null);
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) return Optional.empty();

            String email = getStringClaim(claims, "email");
            String phone = getStringClaim(claims, "phone_number");
            String username = getStringClaim(claims, "cognito:username");
            if (username == null) username = getStringClaim(claims, "username");
            if (username == null) username = email != null ? email : sub;

            return Optional.of(CognitoUserClaims.builder()
                    .sub(sub)
                    .email(email != null ? email : "")
                    .phone(phone)
                    .username(username)
                    .groups(extractGroups(claims))
                    .build());
        } catch (Exception e) {
            log.warn("D2C Cognito JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static List<String> extractGroups(JWTClaimsSet claims) {
        Object raw = claims.getClaim("cognito:groups");
        if (raw == null) return Collections.emptyList();
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) out.add(o.toString());
            }
            return out;
        }
        return List.of(raw.toString());
    }

    private static String getStringClaim(JWTClaimsSet claims, String name) {
        Object val = claims.getClaim(name);
        return val != null ? val.toString() : null;
    }
}
