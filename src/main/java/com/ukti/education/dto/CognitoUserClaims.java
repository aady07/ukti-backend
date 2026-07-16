package com.ukti.education.dto;

import java.util.Collections;
import java.util.List;

/**
 * Cognito JWT claims. Hand-written factory (no Lombok {@code @Builder}) so IDEs
 * without annotation processing still resolve construction.
 */
public class CognitoUserClaims {

    private String sub;
    private String email;
    private String phone;
    private String username;
    private List<String> groups = Collections.emptyList();

    public CognitoUserClaims() {}

    public CognitoUserClaims(String sub, String email, String phone, String username, List<String> groups) {
        this.sub = sub;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.groups = groups != null ? groups : Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasGroup(String group) {
        return groups != null && groups.stream().anyMatch(g -> group.equalsIgnoreCase(g));
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups != null ? groups : Collections.emptyList();
    }

    public static final class Builder {
        private String sub;
        private String email;
        private String phone;
        private String username;
        private List<String> groups = Collections.emptyList();

        public Builder sub(String sub) {
            this.sub = sub;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder groups(List<String> groups) {
            this.groups = groups;
            return this;
        }

        public CognitoUserClaims build() {
            return new CognitoUserClaims(sub, email, phone, username, groups);
        }
    }
}
