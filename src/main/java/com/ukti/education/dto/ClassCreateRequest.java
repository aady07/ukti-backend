package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassCreateRequest {

    @NotBlank(message = "name is required")
    private String name;

    /** Optional: assign existing teacher to this class */
    private String teacherId;
}
