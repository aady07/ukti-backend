package com.ukti.education.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ukti.education.config.MediaStorageProperties;
import com.ukti.education.dto.MediaPresignResponse;
import com.ukti.education.dto.StudentActivityMediaItemResponse;
import com.ukti.education.dto.StudentActivityMediaListResponse;
import com.ukti.education.entity.User;
import com.ukti.education.entity.UserActivityProgress;
import com.ukti.education.repository.UserActivityProgressRepository;
import com.ukti.education.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class ActivityMediaService {

    public static final String META_IMAGE_KEY = "mediaImageKey";
    public static final String META_AUDIO_KEY = "mediaAudioKey";

    private final MediaStorageProperties properties;
    private final ObjectProvider<S3Presigner> presignerProvider;
    private final UserRepository userRepository;
    private final UserActivityProgressRepository progressRepository;

    private S3Presigner presignerOrThrow() {
        S3Presigner p = presignerProvider.getIfAvailable();
        if (!properties.isEnabled() || p == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Activity media storage is not configured (set ukti.media.enabled=true and AWS credentials)");
        }
        return p;
    }

    private String sanitizeSegment(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String buildObjectKey(UUID schoolId, UUID userId, String unitSlug, String activitySlug, String suffix) {
        String prefix = properties.getKeyPrefix() == null ? "" : properties.getKeyPrefix().trim();
        String base = String.format("schools/%s/users/%s/units/%s/activities/%s/%s",
                schoolId, userId,
                sanitizeSegment(unitSlug),
                sanitizeSegment(activitySlug),
                suffix);
        if (prefix.isEmpty()) {
            return base;
        }
        return prefix + "/" + base;
    }

    /**
     * Presign PUT URLs for direct browser upload (no file bytes through this API).
     */
    public MediaPresignResponse presignUpload(
            UUID userId,
            UUID schoolId,
            String unitSlug,
            String activitySlug,
            String imageContentType,
            String audioContentType) {

        S3Presigner presigner = presignerOrThrow();
        String bucket = properties.getBucket();
        int expiresSec = 15 * 60;

        boolean wantImage = imageContentType != null && !imageContentType.isBlank();
        boolean wantAudio = audioContentType != null && !audioContentType.isBlank();
        if (!wantImage && !wantAudio) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide imageContentType and/or audioContentType");
        }

        String imageKey = null;
        String imageUrl = null;
        String audioKey = null;
        String audioUrl = null;
        UUID fileId = UUID.randomUUID();

        if (wantImage) {
            imageKey = buildObjectKey(schoolId, userId, unitSlug, activitySlug, fileId + "-image.jpg");
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(imageKey)
                    .contentType(imageContentType.trim())
                    .build();
            imageUrl = presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expiresSec))
                    .putObjectRequest(put)
                    .build()).url().toExternalForm();
        }
        if (wantAudio) {
            String ext = audioContentType.contains("webm") ? "webm" : "m4a";
            audioKey = buildObjectKey(schoolId, userId, unitSlug, activitySlug, fileId + "-audio." + ext);
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(audioKey)
                    .contentType(audioContentType.trim())
                    .build();
            audioUrl = presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expiresSec))
                    .putObjectRequest(put)
                    .build()).url().toExternalForm();
        }

        return new MediaPresignResponse(imageKey, imageUrl, audioKey, audioUrl, expiresSec);
    }

    public String presignGet(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        S3Presigner presigner = presignerOrThrow();
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(get)
                .build()).url().toExternalForm();
    }

    /**
     * List activity rows that have stored media keys, with short-lived view URLs for admins.
     */
    public StudentActivityMediaListResponse listForStudent(UUID schoolId, UUID studentUserId) {
        Optional<User> u = userRepository.findById(studentUserId);
        if (u.isEmpty() || !"student".equals(u.get().getUserType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        if (u.get().getSchoolUuid() == null || !schoolId.equals(u.get().getSchoolUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not in this school");
        }

        if (!properties.isEnabled() || presignerProvider.getIfAvailable() == null) {
            return StudentActivityMediaListResponse.builder().items(List.of()).build();
        }

        List<UserActivityProgress> all = progressRepository.findByUserIdOrderByCompletedAtDesc(studentUserId);
        List<StudentActivityMediaItemResponse> items = new ArrayList<>();
        for (UserActivityProgress p : all) {
            Map<String, Object> meta = p.getMetadata();
            if (meta == null || meta.isEmpty()) {
                continue;
            }
            Object ik = meta.get(META_IMAGE_KEY);
            Object ak = meta.get(META_AUDIO_KEY);
            if (ik == null && ak == null) {
                continue;
            }
            String imageUrl = ik != null ? presignGet(String.valueOf(ik)) : null;
            String audioUrl = ak != null ? presignGet(String.valueOf(ak)) : null;
            Map<String, Object> safeMeta = new HashMap<>(meta);
            items.add(StudentActivityMediaItemResponse.builder()
                    .unitSlug(p.getUnitSlug())
                    .activitySlug(p.getActivitySlug())
                    .completedAt(p.getCompletedAt().toString())
                    .imageUrl(imageUrl)
                    .audioUrl(audioUrl)
                    .metadata(safeMeta)
                    .build());
        }
        return StudentActivityMediaListResponse.builder().items(items).build();
    }
}
