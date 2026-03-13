package com.ukti.education.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddStudentsRequest {

    @NotEmpty(message = "students list is required")
    @Valid
    private List<StudentInput> students;

    @Data
    public static class StudentInput {
        @jakarta.validation.constraints.NotBlank(message = "rollNumber is required")
        private String rollNumber;
        @jakarta.validation.constraints.NotBlank(message = "name is required")
        private String name;
    }
}
