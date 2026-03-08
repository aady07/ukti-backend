package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CognitoUserClaims {

    private String sub;      // Cognito user ID (unique)
    private String email;
    private String phone;
    private String username;
}
