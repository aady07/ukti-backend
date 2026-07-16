package com.ukti.education.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukti.education.service.CurriculumCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/curriculum")
@RequiredArgsConstructor
public class CurriculumCatalogController {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final CurriculumCatalogService curriculumCatalogService;

    @GetMapping("/catalog")
    public ResponseEntity<?> getCatalog(
            @RequestParam(required = false) String classLevel,
            @RequestParam(required = false, defaultValue = "full") String view) {
        boolean summary = "summary".equalsIgnoreCase(view != null ? view.trim() : "full");
        var catalogOpt = summary
                ? curriculumCatalogService.getCatalogSummaryForClassLevel(classLevel)
                : curriculumCatalogService.getCatalogForClassLevel(classLevel);
        return catalogOpt
                .map(this::toResponseBody)
                .map(body -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                        .body(body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<?> getModule(@PathVariable String moduleId) {
        return curriculumCatalogService.getModule(moduleId)
                .map(this::toResponseBody)
                .map(body -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                        .body(body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** ponytail: Spring serializes raw JsonNode as node metadata; convert to Map first */
    private Object toResponseBody(JsonNode node) {
        return MAPPER.convertValue(node, Object.class);
    }
}
