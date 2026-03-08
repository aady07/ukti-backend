package com.ukti.education.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "cognitoSub is required")
    private String cognitoSub;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String username;

    private String displayName;

    private String schoolId;
}
