package com.ukti.education.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ukti.education.entity.CurriculumModule;
import com.ukti.education.entity.CurriculumRelease;
import com.ukti.education.repository.CurriculumModuleRepository;
import com.ukti.education.repository.CurriculumReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurriculumCatalogService {

    private static final String CDN_ORIGIN = "https://d1194rs9ausm91.cloudfront.net";
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final CurriculumModuleRepository curriculumModuleRepository;
    private final CurriculumReleaseRepository curriculumReleaseRepository;

    private static final List<String> LEVEL_ORDER = List.of(
            "nursery", "lkg", "ukg", "grade1", "grade2", "grade3", "grade4", "grade5", "grade6", "grade7", "laams"
    );

    public Optional<JsonNode> getFullCatalog() {
        List<CurriculumRelease> releases = new ArrayList<>();
        for (String level : LEVEL_ORDER) {
            curriculumReleaseRepository.findTopByClassLevelOrderByVersionDesc(level)
                    .ifPresent(releases::add);
        }
        if (releases.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buildCatalogFromReleases(releases));
    }

    public Optional<JsonNode> getCatalogForClassLevel(String classLevel) {
        if (classLevel == null || classLevel.isBlank()) {
            return getFullCatalog();
        }
        String normalized = classLevel.trim().toLowerCase();
        return curriculumReleaseRepository.findTopByClassLevelOrderByVersionDesc(normalized)
                .map(release -> {
                    ObjectNode catalog = MAPPER.createObjectNode();
                    catalog.put("version", 1);
                    catalog.put("generatedAt", Instant.now().toString());
                    catalog.put("cdnOrigin", CDN_ORIGIN);
                    ArrayNode levels = catalog.putArray("classLevels");
                    levels.add(release.getPayload());
                    return (JsonNode) catalog;
                });
    }

    /** Catalog without per-activity `questions[]` — smaller payload for hub/progress. */
    public Optional<JsonNode> getCatalogSummaryForClassLevel(String classLevel) {
        return getCatalogForClassLevel(classLevel).map(this::stripQuestionsFromCatalog);
    }

    private JsonNode stripQuestionsFromCatalog(JsonNode catalog) {
        if (!catalog.isObject()) return catalog;
        ObjectNode copy = catalog.deepCopy();
        JsonNode levels = copy.path("classLevels");
        if (!levels.isArray()) return copy;
        for (JsonNode level : levels) {
            stripQuestionsFromLevel(level);
        }
        return copy;
    }

    private void stripQuestionsFromLevel(JsonNode level) {
        if (!level.isObject()) return;
        JsonNode modules = level.path("modules");
        if (!modules.isArray()) return;
        for (JsonNode module : modules) {
            if (!module.isObject()) continue;
            JsonNode sections = module.path("sections");
            if (!sections.isArray()) continue;
            for (JsonNode section : sections) {
                if (!section.isObject()) continue;
                JsonNode activities = section.path("activities");
                if (!activities.isArray()) continue;
                for (JsonNode activity : activities) {
                    if (activity instanceof ObjectNode obj) {
                        obj.remove("questions");
                    }
                }
            }
        }
    }

    public Optional<JsonNode> getModule(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return Optional.empty();
        }
        return curriculumModuleRepository.findById(moduleId.trim())
                .map(CurriculumModule::getPayload);
    }

    public Optional<CurriculumModule> getModuleEntity(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return Optional.empty();
        }
        return curriculumModuleRepository.findById(moduleId.trim());
    }

    public Optional<Map<String, List<String>>> getSectionActivitiesForModule(String moduleId) {
        return getModule(moduleId).flatMap(this::extractSectionActivities);
    }

    public Optional<List<String>> getSectionIdsForModule(String moduleId) {
        return getModule(moduleId).flatMap(this::extractSectionIds);
    }

    public Map<String, Integer> getTrackableTotalsForClassLevel(String classLevel) {
        if (classLevel == null || classLevel.isBlank()) {
            return Map.of();
        }
        List<CurriculumModule> modules = curriculumModuleRepository.findByClassLevelOrderByModuleIdAsc(classLevel.trim().toLowerCase());
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (CurriculumModule module : modules) {
            int count = countTrackableActivities(module.getPayload());
            if (count > 0) {
                totals.put(module.getModuleId(), count);
            }
        }
        return totals;
    }

    private JsonNode buildCatalogFromReleases(List<CurriculumRelease> releases) {
        ObjectNode catalog = MAPPER.createObjectNode();
        catalog.put("version", 1);
        catalog.put("generatedAt", Instant.now().toString());
        catalog.put("cdnOrigin", CDN_ORIGIN);
        ArrayNode levels = catalog.putArray("classLevels");
        for (CurriculumRelease release : releases) {
            levels.add(release.getPayload());
        }
        return catalog;
    }

    private Optional<Map<String, List<String>>> extractSectionActivities(JsonNode payload) {
        JsonNode orchestration = payload.path("orchestration");
        JsonNode sectionActivities = orchestration.path("sectionActivities");
        if (!sectionActivities.isObject()) {
            return Optional.empty();
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = sectionActivities.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isArray()) continue;
            List<String> slugs = new ArrayList<>();
            entry.getValue().forEach(node -> {
                if (node.isTextual() && !node.asText().isBlank()) {
                    slugs.add(node.asText().trim());
                }
            });
            if (!slugs.isEmpty()) {
                out.put(entry.getKey(), slugs);
            }
        }
        return out.isEmpty() ? Optional.empty() : Optional.of(out);
    }

    private Optional<List<String>> extractSectionIds(JsonNode payload) {
        JsonNode sectionIds = payload.path("orchestration").path("sectionIds");
        if (!sectionIds.isArray()) {
            return Optional.empty();
        }
        List<String> ids = new ArrayList<>();
        sectionIds.forEach(node -> {
            if (node.isTextual() && !node.asText().isBlank()) {
                ids.add(node.asText().trim());
            }
        });
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids);
    }

    private int countTrackableActivities(JsonNode payload) {
        JsonNode activityIds = payload.path("trackable").path("activityIds");
        if (!activityIds.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode ignored : activityIds) {
            count++;
        }
        return count;
    }

    public int countTrackableActivitiesForModule(String moduleId) {
        return getModule(moduleId).map(this::countTrackableActivities).orElse(0);
    }

    public Set<String> listModuleIdsForClassLevel(String classLevel) {
        return curriculumModuleRepository.findByClassLevelOrderByModuleIdAsc(classLevel).stream()
                .map(CurriculumModule::getModuleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
