package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ClassModuleRunStartResponse {
    private UUID runId;
    private String status;
    private Integer activeSectionIndex;
    private String activeSectionId;
    private List<GateItem> sectionGateStatus;

    @Data
    @Builder
    public static class GateItem {
        private String sectionId;
        private int sectionIndex;
        private String status;
    }
}
