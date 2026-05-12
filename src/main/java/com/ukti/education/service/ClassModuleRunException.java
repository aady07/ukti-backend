package com.ukti.education.service;

public class ClassModuleRunException extends RuntimeException {
    private final String code;

    public ClassModuleRunException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
