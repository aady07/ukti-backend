package com.ukti.education.controller;

import com.ukti.education.dto.CampaignDtos;
import com.ukti.education.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/campaign")
@RequiredArgsConstructor
@Slf4j
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(@RequestBody CampaignDtos.StartSessionRequest request) {
        try {
            return ResponseEntity.ok(campaignService.startSession(request != null ? request : new CampaignDtos.StartSessionRequest()));
        } catch (Exception e) {
            log.error("campaign_start_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to start campaign session"));
        }
    }

    @PostMapping("/sessions/{sessionId}/events")
    public ResponseEntity<?> ingestEvents(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CampaignDtos.EventsBatchRequest request) {
        try {
            campaignService.ingestEvents(sessionId, request);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("campaign_events_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to record events"));
        }
    }

    @PostMapping("/sessions/{sessionId}/parts")
    public ResponseEntity<?> upsertPart(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CampaignDtos.PartAttemptRequest request) {
        try {
            return ResponseEntity.ok(campaignService.upsertPart(sessionId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("campaign_part_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to save part attempt"));
        }
    }

    @PostMapping("/sessions/{sessionId}/parts/{partIndex}/score")
    public ResponseEntity<?> saveScore(
            @PathVariable UUID sessionId,
            @PathVariable int partIndex,
            @Valid @RequestBody CampaignDtos.PartScoreRequest request) {
        try {
            return ResponseEntity.ok(campaignService.savePartScore(sessionId, partIndex, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("campaign_score_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to save score"));
        }
    }

    @GetMapping("/sessions/{sessionId}/parts")
    public ResponseEntity<?> listParts(@PathVariable UUID sessionId) {
        try {
            return ResponseEntity.ok(campaignService.listParts(sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/parts/{partIndex}")
    public ResponseEntity<?> getPart(@PathVariable UUID sessionId, @PathVariable int partIndex) {
        try {
            return campaignService.getPart(sessionId, partIndex)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new UserController.ErrorResponse("NOT_FOUND", "Part not found")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserController.ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    @PostMapping("/auth/link")
    public ResponseEntity<?> linkAuth(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CampaignDtos.AuthLinkRequest request) {
        try {
            CampaignDtos.AuthLinkRequest body = request != null ? request : new CampaignDtos.AuthLinkRequest();
            return ResponseEntity.ok(campaignService.linkAuth(authorization, body));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error("campaign_auth_link_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to link campaign auth"));
        }
    }

    @GetMapping("/me/attempts")
    public ResponseEntity<?> myAttempts(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            return ResponseEntity.ok(campaignService.listMyAttempts(authorization));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserController.ErrorResponse("UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error("campaign_me_attempts_failed {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Failed to load attempts"));
        }
    }

    @GetMapping("/admin/summary")
    public ResponseEntity<?> adminSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            campaignService.assertCampaignAdmin(authorization);
            return ResponseEntity.ok(campaignService.adminSummary());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        }
    }

    @GetMapping("/admin/leads")
    public ResponseEntity<?> adminLeads(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            campaignService.assertCampaignAdmin(authorization);
            return ResponseEntity.ok(campaignService.adminLeads());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        }
    }

    @GetMapping("/admin/sessions")
    public ResponseEntity<?> adminSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            campaignService.assertCampaignAdmin(authorization);
            return ResponseEntity.ok(campaignService.adminSessions());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        }
    }

    @GetMapping("/admin/sessions/{sessionId}")
    public ResponseEntity<?> adminSessionDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID sessionId) {
        try {
            campaignService.assertCampaignAdmin(authorization);
            return ResponseEntity.ok(campaignService.adminSessionDetail(sessionId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserController.ErrorResponse("NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/admin/schools")
    public ResponseEntity<?> adminSchools(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            campaignService.assertCampaignAdmin(authorization);
            return ResponseEntity.ok(campaignService.adminSchoolsOverview());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        }
    }

    @GetMapping("/admin/schools/{schoolId}")
    public ResponseEntity<?> adminSchoolDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID schoolId) {
        try {
            campaignService.assertCampaignAdmin(authorization);
            return ResponseEntity.ok(campaignService.adminSchoolDetail(schoolId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserController.ErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserController.ErrorResponse("NOT_FOUND", e.getMessage()));
        }
    }
}
