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

/**
 * Creates and verifies JWT tokens for teachers.
 * Uses HS256 with a shared secret.
 */
@Service
@Slf4j
public class TeacherJwtService {

    private static final String CLAIM_TEACHER_ID = "teacherId";
    private static final String CLAIM_SCHOOL_ID = "schoolId";
    private static final long EXPIRY_SECONDS = 7 * 24 * 60 * 60; // 7 days

    private final SecretKey secretKey;

    public TeacherJwtService(
            @Value("${ukti.teacher.jwt-secret:ukti-teacher-jwt-secret-change-in-production-min-32-chars}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = new SecretKeySpec(keyBytes, 0, Math.min(keyBytes.length, 32), "HmacSHA256");
    }

    public String createToken(UUID teacherId, UUID schoolId) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(EXPIRY_SECONDS);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(teacherId.toString())
                    .claim(CLAIM_TEACHER_ID, teacherId.toString())
                    .claim(CLAIM_SCHOOL_ID, schoolId.toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            JWSSigner signer = new MACSigner(secretKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create teacher token", e);
        }
    }

    public Optional<TeacherTokenClaims> validateAndExtract(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return Optional.empty();
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : bearerToken.trim();
        if (token.isEmpty()) return Optional.empty();

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKey);
            if (!signedJWT.verify(verifier)) return Optional.empty();

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }

            String teacherId = (String) claims.getClaim(CLAIM_TEACHER_ID);
            String schoolId = (String) claims.getClaim(CLAIM_SCHOOL_ID);
            if (teacherId == null || schoolId == null) return Optional.empty();

            return Optional.of(new TeacherTokenClaims(
                    UUID.fromString(teacherId),
                    UUID.fromString(schoolId)
            ));
        } catch (Exception e) {
            log.warn("Teacher JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record TeacherTokenClaims(UUID teacherId, UUID schoolId) {}
}
