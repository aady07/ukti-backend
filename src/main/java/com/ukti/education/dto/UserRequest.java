package com.ukti.education.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {

    private String cognitoSub;  // Required for individual/school_admin; null for students

    private String email;  // Required for individual/school_admin; null for students

    private String phone;

    private String username;

    private String displayName;

    private String schoolId;  // Legacy

    /** "individual" (default) or "organization" (school admin signup) */
    private String signupType;

    /** Required when signupType=organization; school name */
    private String organizationName;
}
