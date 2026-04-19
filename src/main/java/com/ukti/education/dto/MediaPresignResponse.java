package com.ukti.education.dto;

public record MediaPresignResponse(
        String imageKey,
        String imageUploadUrl,
        String audioKey,
        String audioUploadUrl,
        int expiresInSeconds
) {}
