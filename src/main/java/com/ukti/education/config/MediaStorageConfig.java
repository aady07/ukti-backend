package com.ukti.education.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(MediaStorageProperties.class)
public class MediaStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ukti.media", name = "enabled", havingValue = "true")
    public S3Presigner s3Presigner(MediaStorageProperties props) {
        if (props.getBucket() == null || props.getBucket().isBlank()) {
            throw new IllegalStateException("ukti.media.bucket must be set when ukti.media.enabled=true");
        }
        return S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
