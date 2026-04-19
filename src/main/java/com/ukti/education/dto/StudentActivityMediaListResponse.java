package com.ukti.education.dto;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentActivityMediaListResponse {

    List<StudentActivityMediaItemResponse> items;
}
