package com.ukti.education.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** HS256 JWT for DB-backed super admins (no Cognito). */
@Service
@Slf4j
public class SuperAdminJwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final long EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    private final SecretKey secretKey;

    public SuperAdminJwtService(
            @Value("${ukti.super-admin.jwt-secret:ukti-super-admin-jwt-secret-change-in-production-min-32}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = new SecretKeySpec(keyBytes, 0, Math.min(keyBytes.length, 32), "HmacSHA256");
    }

    public String createToken(UUID userId, String email) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("email", email)
                    .claim(CLAIM_ROLE, ROLE_SUPER_ADMIN)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(EXPIRY_SECONDS)))
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            JWSSigner signer = new MACSigner(secretKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create super-admin token", e);
        }
    }

    public Optional<SuperAdminClaims> validateAndExtract(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return Optional.empty();
        String token = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7).trim()
                : bearerToken.trim();
        if (token.isEmpty()) return Optional.empty();
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKey);
            if (!signedJWT.verify(verifier)) return Optional.empty();
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }
            if (!ROLE_SUPER_ADMIN.equals(claims.getStringClaim(CLAIM_ROLE))) {
                return Optional.empty();
            }
            String sub = claims.getSubject();
            String email = claims.getStringClaim("email");
            if (sub == null || sub.isBlank()) return Optional.empty();
            return Optional.of(new SuperAdminClaims(UUID.fromString(sub), email));
        } catch (Exception e) {
            log.warn("Super-admin JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record SuperAdminClaims(UUID userId, String email) {}
}
