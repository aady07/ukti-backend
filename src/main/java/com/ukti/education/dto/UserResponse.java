package com.ukti.education.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String cognitoSub;
    private String email;
    private String phone;
    private String username;
    private String displayName;
    private String schoolId;  // Legacy
    /** "individual" | "school_admin" | "student" */
    private String userType;
    /** For school_admin and students; school UUID (use this for school context) */
    private String schoolUuid;
    /** School name; for school_admin */
    private String schoolName;
    private Instant createdAt;
    private Instant updatedAt;
}
