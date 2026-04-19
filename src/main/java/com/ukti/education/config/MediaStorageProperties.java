package com.ukti.education.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * S3 bucket for student activity captures (presigned PUT/GET; no binary through API).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ukti.media")
public class MediaStorageProperties {

    /** When false, presign endpoints return 503 (local dev without AWS). */
    private boolean enabled = false;

    private String bucket = "";

    private String region = "ap-south-1";

    /** Optional key prefix, no leading/trailing slashes. */
    private String keyPrefix = "activity-media";
}
