package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ClassModuleActivityCompleteRequest {
    @NotBlank
    private String sectionId;
    private Integer challengeIndex;
    @NotBlank
    private String result;
    private BigDecimal score;
    private Map<String, Object> metadata;
}
