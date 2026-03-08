package com.ukti.education.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ActivityCompleteRequest {

    private Map<String, Object> metadata;  // e.g. { "correct": true } for assessments
}
