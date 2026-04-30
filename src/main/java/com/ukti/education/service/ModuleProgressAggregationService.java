package com.ukti.education.service;

import com.ukti.education.dto.ClassModulesProgressResponse;
import com.ukti.education.dto.StudentModulesProgressResponse;
import com.ukti.education.entity.SchoolClass;
import com.ukti.education.entity.User;
import com.ukti.education.entity.UserActivityProgress;
import com.ukti.education.repository.SchoolClassRepository;
import com.ukti.education.repository.UserActivityProgressRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModuleProgressAggregationService {

    private static final String USER_TYPE_STUDENT = "student";

    private final UserRepository userRepository;
    private final UserActivityProgressRepository progressRepository;
    private final SchoolClassRepository schoolClassRepository;

    public Optional<StudentModulesProgressResponse> getStudentModuleProgress(UUID schoolId, UUID classId, String rollNumber) {
        Optional<User> studentOpt = userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(
                schoolId, classId, rollNumber, USER_TYPE_STUDENT);
        if (studentOpt.isEmpty()) {
            return Optional.empty();
        }

        User student = studentOpt.get();
        List<UserActivityProgress> classRows = progressRepository.findBySchoolAndClassStudents(schoolId, classId);
        List<UserActivityProgress> studentRows = classRows.stream()
                .filter(row -> row.getUser() != null && student.getId().equals(row.getUser().getId()))
                .toList();

        Map<String, Integer> classTotalsByModule = buildModuleTotals(classRows);
        Map<String, Integer> completedByModule = buildCompletedCounts(studentRows);
        Set<String> moduleIds = mergeModuleIds(classTotalsByModule.keySet(), completedByModule.keySet());
        String classLevel = resolveClassLevel(classId, classRows);

        List<StudentModulesProgressResponse.ModuleProgressItem> modules = new ArrayList<>();
        for (String moduleId : moduleIds) {
            int completedCount = completedByModule.getOrDefault(moduleId, 0);
            int totalCount = Math.max(classTotalsByModule.getOrDefault(moduleId, 0), completedCount);
            int percent = totalCount > 0 ? Math.min(100, (int) Math.round(100.0 * completedCount / totalCount)) : 0;
            modules.add(StudentModulesProgressResponse.ModuleProgressItem.builder()
                    .moduleId(moduleId)
                    .completedCount(completedCount)
                    .totalCount(totalCount)
                    .percent(percent)
                    .build());
        }

        return Optional.of(StudentModulesProgressResponse.builder()
                .classId(classId.toString())
                .classLevel(classLevel)
                .rollNumber(rollNumber)
                .modules(modules)
                .build());
    }

    public ClassModulesProgressResponse getClassModuleProgress(UUID schoolId, UUID classId) {
        List<User> students = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(
                schoolId, classId, USER_TYPE_STUDENT);
        List<UserActivityProgress> classRows = progressRepository.findBySchoolAndClassStudents(schoolId, classId);

        Map<String, Integer> classTotalsByModule = buildModuleTotals(classRows);
        Map<UUID, List<UserActivityProgress>> rowsByUser = classRows.stream()
                .filter(row -> row.getUser() != null)
                .collect(Collectors.groupingBy(row -> row.getUser().getId()));
        String classLevel = resolveClassLevel(classId, classRows);

        List<ClassModulesProgressResponse.ModuleProgressItem> modules = new ArrayList<>();
        for (String moduleId : classTotalsByModule.keySet()) {
            int totalCount = classTotalsByModule.getOrDefault(moduleId, 0);
            int studentsWithData = 0;
            int percentTotal = 0;

            for (User student : students) {
                List<UserActivityProgress> studentRows = rowsByUser.getOrDefault(student.getId(), List.of()).stream()
                        .filter(row -> moduleId.equals(extractModuleId(row)))
                        .toList();
                if (studentRows.isEmpty()) continue;
                studentsWithData++;
                int completedCount = buildCompletedCounts(studentRows).getOrDefault(moduleId, 0);
                int percent = totalCount > 0 ? Math.min(100, (int) Math.round(100.0 * completedCount / totalCount)) : 0;
                percentTotal += percent;
            }

            int avgPercent = studentsWithData > 0 ? (int) Math.round((double) percentTotal / studentsWithData) : 0;
            modules.add(ClassModulesProgressResponse.ModuleProgressItem.builder()
                    .moduleId(moduleId)
                    .avgPercent(avgPercent)
                    .studentsWithData(studentsWithData)
                    .build());
        }

        return ClassModulesProgressResponse.builder()
                .classId(classId.toString())
                .classLevel(classLevel)
                .studentsTracked(students.size())
                .modules(modules)
                .build();
    }

    private Map<String, Integer> buildModuleTotals(List<UserActivityProgress> rows) {
        Map<String, Set<String>> logicalKeysByModule = new LinkedHashMap<>();
        for (UserActivityProgress row : rows) {
            String moduleId = extractModuleId(row);
            if (moduleId == null) continue;
            String logicalKey = extractLogicalActivityKey(row);
            if (logicalKey == null) continue;
            logicalKeysByModule.computeIfAbsent(moduleId, ignored -> new HashSet<>()).add(logicalKey);
        }
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : logicalKeysByModule.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().size());
        }
        return totals;
    }

    private Map<String, Integer> buildCompletedCounts(List<UserActivityProgress> rows) {
        Map<String, Map<String, StepState>> stateByModule = new HashMap<>();
        for (UserActivityProgress row : rows) {
            String moduleId = extractModuleId(row);
            if (moduleId == null) continue;
            String logicalKey = extractLogicalActivityKey(row);
            if (logicalKey == null) continue;
            String activitySlug = row.getActivitySlug();
            Map<String, StepState> byLogical = stateByModule.computeIfAbsent(moduleId, ignored -> new HashMap<>());
            StepState state = byLogical.computeIfAbsent(logicalKey, ignored -> new StepState());
            if (activitySlug == null) continue;
            if (activitySlug.endsWith("-show")) state.show = true;
            else if (activitySlug.endsWith("-say")) state.say = true;
            else state.full = true;
            state.baseSlug = ActivityLogicalCompletionService.parseBaseSlug(activitySlug);
        }

        Map<String, Integer> completedCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, StepState>> moduleEntry : stateByModule.entrySet()) {
            int completed = 0;
            for (StepState state : moduleEntry.getValue().values()) {
                if (isCompleted(state)) completed++;
            }
            completedCounts.put(moduleEntry.getKey(), completed);
        }
        return completedCounts;
    }

    private boolean isCompleted(StepState state) {
        if (state.full) return true;
        String baseSlug = state.baseSlug != null ? state.baseSlug : "";
        if (isSayOnlyBase(baseSlug)) return state.say;
        return state.show && state.say;
    }

    private boolean isSayOnlyBase(String baseSlug) {
        return ActivityLogicalCompletionService.isSayOnlyBase(baseSlug) || baseSlug.startsWith("assessment-");
    }

    private String resolveClassLevel(UUID classId, List<UserActivityProgress> rows) {
        Map<String, Long> byClassLevel = rows.stream()
                .map(this::extractClassLevel)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        if (!byClassLevel.isEmpty()) {
            return byClassLevel.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        return schoolClassRepository.findById(classId)
                .map(SchoolClass::getName)
                .map(this::normalizeClassLevel)
                .orElse(null);
    }

    private String normalizeClassLevel(String className) {
        if (className == null) return null;
        String normalized = className.trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
        return switch (normalized) {
            case "nursery" -> "nursery";
            case "lkg" -> "lkg";
            case "ukg" -> "ukg";
            case "grade1", "class1", "1" -> "grade1";
            case "grade2", "class2", "2" -> "grade2";
            default -> null;
        };
    }

    private Set<String> mergeModuleIds(Collection<String> first, Collection<String> second) {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(first);
        all.addAll(second);
        return all;
    }

    private String extractModuleId(UserActivityProgress row) {
        Object value = extractMetadataValue(row, "moduleId", "module_id");
        if (!(value instanceof String moduleId)) return null;
        return moduleId.isBlank() ? null : moduleId;
    }

    private String extractClassLevel(UserActivityProgress row) {
        Object value = extractMetadataValue(row, "classLevel", "class_level");
        if (!(value instanceof String classLevel)) return null;
        return classLevel.isBlank() ? null : classLevel;
    }

    private String extractLogicalActivityKey(UserActivityProgress row) {
        Object itemIdValue = extractMetadataValue(row, "moduleItemId", "module_item_id");
        if (itemIdValue instanceof String moduleItemId && !moduleItemId.isBlank()) {
            return moduleItemId;
        }
        String activitySlug = row.getActivitySlug();
        if (activitySlug == null || activitySlug.isBlank()) return null;
        return ActivityLogicalCompletionService.parseBaseSlug(activitySlug);
    }

    private Object extractMetadataValue(UserActivityProgress row, String... keys) {
        Map<String, Object> metadata = row.getMetadata();
        if (metadata == null || metadata.isEmpty()) return null;
        for (String key : keys) {
            if (metadata.containsKey(key)) return metadata.get(key);
        }
        return null;
    }

    private static final class StepState {
        boolean show;
        boolean say;
        boolean full;
        String baseSlug;
    }
}
