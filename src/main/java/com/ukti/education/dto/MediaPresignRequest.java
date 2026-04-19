package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaPresignRequest {

    /** e.g. image/jpeg */
    private String imageContentType;

    /** e.g. audio/webm */
    private String audioContentType;
}
