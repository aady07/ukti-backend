package com.ukti.education.dto;

import lombok.Data;

@Data
public class ClassUpdateRequest {

    private String teacherId;  // UUID of teacher to assign to this class
}
