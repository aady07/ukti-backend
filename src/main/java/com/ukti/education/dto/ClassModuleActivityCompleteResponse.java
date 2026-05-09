package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassModuleActivityCompleteResponse {
    private String studentSectionStatus;
    private String classSectionStatus;
    private String nextAction;
    private RollResolveNextResponse resolvedNext;
}
