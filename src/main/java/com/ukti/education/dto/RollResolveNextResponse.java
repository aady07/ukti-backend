package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RollResolveNextResponse {
    private boolean allowed;
    private String reason;
    private String sectionId;
    private Integer sectionIndex;
    private String activityId;
    private Integer challengeIndex;
    private String studentSectionStatus;
    private String classSectionStatus;
}
