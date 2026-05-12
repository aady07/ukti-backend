package com.ukti.education.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementSessionBatchResponse {

    @Builder.Default
    private boolean ok = true;

    private UUID sessionId;
    private UUID receivedBatchId;
}
