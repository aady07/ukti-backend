package com.ukti.education.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ukti.education.dto.*;
import com.ukti.education.entity.*;
import com.ukti.education.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassModuleRunService {
    private static final JsonNodeFactory JSON_NODES = JsonNodeFactory.instance;

    public static record RunScope(UUID schoolId, UUID classId) {}

    private static final String RUN_ACTIVE = "active";
    private static final String RUN_COMPLETED = "completed";
    private static final String GATE_LOCKED = "locked";
    private static final String GATE_UNLOCKED = "unlocked";
    private static final String GATE_COMPLETED = "completed";
    private static final String STUDENT_NOT_STARTED = "not_started";
    private static final String STUDENT_IN_PROGRESS = "in_progress";
    private static final String STUDENT_COMPLETED = "completed";
    private static final String USER_TYPE_STUDENT = "student";

    private final ClassModuleRunRepository classModuleRunRepository;
    private final ClassSectionGateRepository classSectionGateRepository;
    private final StudentSectionProgressRepository studentSectionProgressRepository;
    private final StudentActivityProgressRepository studentActivityProgressRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public ClassModuleRunStartResponse startOrResume(ClassModuleRunStartRequest request, UUID initiatedBy) {
        if (request.getSectionIds() == null || request.getSectionIds().isEmpty()) {
            throw new ClassModuleRunException("BAD_REQUEST", "sectionIds must not be empty");
        }
        validateSectionIds(request.getSectionIds());
        validateSectionActivities(request.getSectionIds(), request.getSectionActivities());
        Optional<SchoolClass> schoolClass = schoolClassRepository.findById(request.getClassId());
        if (schoolClass.isEmpty() || !schoolClass.get().getSchoolId().equals(request.getSchoolId())) {
            throw new ClassModuleRunException("BAD_REQUEST", "Class not found for school");
        }

        Optional<ClassModuleRun> existing = classModuleRunRepository.findBySchoolIdAndClassIdAndModuleIdAndStatus(
                request.getSchoolId(), request.getClassId(), request.getModuleId(), RUN_ACTIVE);
        if (existing.isPresent()) {
            log.info("run_started existing run reused runId={}", existing.get().getId());
            return toStartResponse(existing.get(), classSectionGateRepository.findByRunIdOrderBySectionIndexAsc(existing.get().getId()));
        }

        String completionRule = normalizeCompletionRule(request.getCompletionRule());
        Integer requestedTarget = request.getCompletionTarget();
        int completionTarget = requestedTarget != null ? requestedTarget : 100;

        ClassModuleRun run = classModuleRunRepository.save(ClassModuleRun.builder()
                .schoolId(request.getSchoolId())
                .classId(request.getClassId())
                .moduleId(request.getModuleId())
                .status(RUN_ACTIVE)
                .activeSectionIndex(0)
                .sectionOrder(request.getSectionIds())
                .sectionActivities(request.getSectionActivities())
                .completionRule(completionRule)
                .completionTarget(completionTarget)
                .createdBy(initiatedBy != null ? initiatedBy : request.getInitiatedBy())
                .startedAt(Instant.now())
                .build());

        List<ClassSectionGate> gates = new ArrayList<>();
        for (int i = 0; i < request.getSectionIds().size(); i++) {
            gates.add(ClassSectionGate.builder()
                    .runId(run.getId())
                    .sectionId(request.getSectionIds().get(i))
                    .sectionIndex(i)
                    .status(i == 0 ? GATE_UNLOCKED : GATE_LOCKED)
                    .completionRule(completionRule)
                    .completionTarget(completionTarget)
                    .unlockedAt(i == 0 ? Instant.now() : null)
                    .build());
        }
        classSectionGateRepository.saveAll(gates);

        List<User> students = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(
                run.getSchoolId(), run.getClassId(), USER_TYPE_STUDENT);
        List<StudentSectionProgress> rows = new ArrayList<>();
        for (User student : students) {
            for (String sectionId : request.getSectionIds()) {
                rows.add(StudentSectionProgress.builder()
                        .runId(run.getId())
                        .studentUserId(student.getId())
                        .rollNumber(student.getRollNumber())
                        .sectionId(sectionId)
                        .status(STUDENT_NOT_STARTED)
                        .attemptCount(0)
                        .build());
            }
        }
        studentSectionProgressRepository.saveAll(rows);

        log.info("run_started runId={} classId={} moduleId={}", run.getId(), run.getClassId(), run.getModuleId());
        return toStartResponse(run, gates);
    }

    @Transactional(readOnly = true)
    public RollResolveNextResponse resolveNext(UUID runId, String rollNumber) {
        ClassModuleRun run = getRunOrThrow(runId);
        if (RUN_COMPLETED.equals(run.getStatus())) {
            return RollResolveNextResponse.builder().allowed(false).reason("run_completed").build();
        }

        Optional<User> student = userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(
                run.getSchoolId(), run.getClassId(), rollNumber, USER_TYPE_STUDENT);
        if (student.isEmpty()) {
            return RollResolveNextResponse.builder().allowed(false).reason("roll_not_in_class").build();
        }

        List<ClassSectionGate> gates = classSectionGateRepository.findByRunIdOrderBySectionIndexAsc(runId);
        Map<String, StudentSectionProgress> progressBySection = studentSectionProgressRepository
                .findByRunIdAndRollNumberOrderBySectionIdAsc(runId, rollNumber)
                .stream()
                .collect(Collectors.toMap(StudentSectionProgress::getSectionId, p -> p));

        for (ClassSectionGate gate : gates) {
            StudentSectionProgress p = progressBySection.get(gate.getSectionId());
            if (p == null || !STUDENT_COMPLETED.equals(p.getStatus())) {
                if (GATE_LOCKED.equals(gate.getStatus())) {
                    return RollResolveNextResponse.builder()
                            .allowed(false)
                            .reason("section_locked")
                            .sectionId(gate.getSectionId())
                            .sectionIndex(gate.getSectionIndex())
                            .studentSectionStatus(p != null ? p.getStatus() : STUDENT_NOT_STARTED)
                            .classSectionStatus(gate.getStatus())
                            .build();
                }
                return buildAllowedResolveResponse(run, gate, p);
            }
        }

        return RollResolveNextResponse.builder().allowed(false).reason("run_completed").build();
    }

    @Transactional
    public ClassModuleActivityCompleteResponse completeActivity(UUID runId, String rollNumber, String activityId, String idempotencyKey, ClassModuleActivityCompleteRequest request) {
        ClassModuleRun run = classModuleRunRepository.findWithLockById(runId)
                .orElseThrow(() -> new ClassModuleRunException("RUN_NOT_FOUND", "Run not found"));
        if (RUN_COMPLETED.equals(run.getStatus())) {
            throw new ClassModuleRunException("RUN_COMPLETED", "Run already completed");
        }

        Optional<User> student = userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(
                run.getSchoolId(), run.getClassId(), rollNumber, USER_TYPE_STUDENT);
        if (student.isEmpty()) throw new ClassModuleRunException("ROLL_NOT_IN_CLASS", "Student roll is not in class roster");

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<StudentActivityProgress> replay = studentActivityProgressRepository.findByRunIdAndIdempotencyKey(runId, idempotencyKey);
            if (replay.isPresent()) {
                StudentActivityProgress previous = replay.get();
                if (!Objects.equals(previous.getRollNumber(), rollNumber)
                        || !Objects.equals(previous.getSectionId(), request.getSectionId())
                        || !Objects.equals(previous.getActivityId(), activityId)
                        || !Objects.equals(previous.getChallengeIndex(), request.getChallengeIndex())) {
                    throw new ClassModuleRunException("IDEMPOTENCY_CONFLICT", "Idempotency key reused with different payload");
                }
                RollResolveNextResponse resolved = resolveNext(runId, rollNumber);
                String replayStudentStatus = resolved.getStudentSectionStatus() != null ? resolved.getStudentSectionStatus() : STUDENT_IN_PROGRESS;
                return ClassModuleActivityCompleteResponse.builder()
                        .studentSectionStatus(replayStudentStatus)
                        .classSectionStatus(resolved.getClassSectionStatus())
                        .nextAction("idempotent_replay")
                        .resolvedNext(resolved)
                        .build();
            }
        }

        ClassSectionGate gate = classSectionGateRepository.findWithLockByRunIdAndSectionId(runId, request.getSectionId())
                .orElseThrow(() -> new ClassModuleRunException("INVALID_SECTION_FOR_ROLL", "Section does not belong to this run"));
        if (GATE_LOCKED.equals(gate.getStatus())) {
            throw new ClassModuleRunException("SECTION_LOCKED", "Section is still locked");
        }

        RollResolveNextResponse expected = resolveNext(runId, rollNumber);
        if (!expected.isAllowed()) {
            if ("section_locked".equals(expected.getReason())) {
                throw new ClassModuleRunException("SECTION_LOCKED", "Next section is locked for this roll");
            }
            throw new ClassModuleRunException("INVALID_SECTION_FOR_ROLL", "Roll is not allowed to submit at this section");
        }
        if (!request.getSectionId().equals(expected.getSectionId())) {
            throw new ClassModuleRunException("INVALID_SECTION_FOR_ROLL", "Roll cannot submit out-of-order section");
        }

        studentActivityProgressRepository.save(StudentActivityProgress.builder()
                .runId(runId)
                .studentUserId(student.get().getId())
                .rollNumber(rollNumber)
                .sectionId(request.getSectionId())
                .activityId(activityId)
                .challengeIndex(request.getChallengeIndex())
                .status(request.getResult())
                .score(request.getScore())
                .idempotencyKey(idempotencyKey)
                .metadata(request.getMetadata())
                .build());

        StudentSectionProgress studentSection = studentSectionProgressRepository
                .findWithLockByRunIdAndRollNumberAndSectionId(runId, rollNumber, request.getSectionId())
                .orElseThrow(() -> new ClassModuleRunException("INVALID_SECTION_FOR_ROLL", "Student section progress missing"));

        studentSection.setAttemptCount(studentSection.getAttemptCount() + 1);
        studentSection.setLastActivityId(activityId);
        studentSection.setLastChallengeIndex(request.getChallengeIndex());
        studentSection.setMetadata(request.getMetadata());
        if (studentSection.getStartedAt() == null) studentSection.setStartedAt(Instant.now());
        studentSection.setStatus(STUDENT_IN_PROGRESS);

        String normalizedResult = request.getResult().trim().toLowerCase(Locale.ROOT);
        if ("passed".equals(normalizedResult) || "skipped".equals(normalizedResult)) {
            studentSection.setStatus(STUDENT_COMPLETED);
            studentSection.setCompletedAt(Instant.now());
        }
        studentSectionProgressRepository.save(studentSection);

        Instant interactionAt = Instant.now();
        run.setLastRollNumber(rollNumber);
        run.setLastActivityId(activityId);
        run.setLastSectionId(request.getSectionId());
        run.setLastInteractionAt(interactionAt);
        classModuleRunRepository.save(run);
        log.info("run_last_interaction_updated runId={} rollNumber={} sectionId={} activityId={} at={}",
                runId, rollNumber, request.getSectionId(), activityId, interactionAt);

        refreshGateAndRunState(run, gate);
        RollResolveNextResponse resolvedNext = resolveNext(runId, rollNumber);
        String nextAction = RUN_COMPLETED.equals(run.getStatus())
                ? "module_completed"
                : (resolvedNext.isAllowed() && !Objects.equals(resolvedNext.getSectionId(), request.getSectionId())
                ? "next_section_unlocked" : "same_section_next_activity");

        return ClassModuleActivityCompleteResponse.builder()
                .studentSectionStatus(studentSection.getStatus())
                .classSectionStatus(gate.getStatus())
                .nextAction(nextAction)
                .resolvedNext(resolvedNext)
                .build();
    }

    @Transactional(readOnly = true)
    public ClassSectionSummaryResponse sectionSummary(UUID runId, String sectionId) {
        ClassModuleRun run = getRunOrThrow(runId);
        ClassSectionGate gate = classSectionGateRepository.findByRunIdAndSectionId(runId, sectionId)
                .orElseThrow(() -> new ClassModuleRunException("NOT_FOUND", "Section not found"));
        int total = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(
                run.getSchoolId(), run.getClassId(), USER_TYPE_STUDENT).size();
        long completed = studentSectionProgressRepository.countByRunIdAndSectionIdAndStatus(runId, sectionId, STUDENT_COMPLETED);
        int pct = total == 0 ? 0 : (int) Math.floor((completed * 100.0) / total);

        List<ClassSectionSummaryResponse.StudentItem> students = studentSectionProgressRepository
                .findByRunIdAndSectionId(runId, sectionId).stream()
                .map(p -> ClassSectionSummaryResponse.StudentItem.builder()
                        .rollNumber(p.getRollNumber())
                        .status(p.getStatus())
                        .lastActivity(p.getLastActivityId())
                        .startedAt(p.getStartedAt() != null ? p.getStartedAt().toString() : null)
                        .completedAt(p.getCompletedAt() != null ? p.getCompletedAt().toString() : null)
                        .build())
                .toList();

        return ClassSectionSummaryResponse.builder()
                .totalStudents(total)
                .completedStudents(completed)
                .completionPct(pct)
                .gateStatus(gate.getStatus())
                .students(students)
                .build();
    }

    @Transactional(readOnly = true)
    public ClassModuleRunStatusResponse runStatus(UUID runId) {
        return buildStatusResponse(getRunOrThrow(runId));
    }

    /**
     * PATCH body {@code { "classRuntimeState": { ... } }} — deep-merges into stored JSON (other keys preserved).
     */
    @Transactional
    public ClassModuleRunStatusResponse patchRun(UUID runId, ClassModuleRunPatchRequest request) {
        if (request == null || request.getClassRuntimeState() == null || request.getClassRuntimeState().isNull()) {
            throw new ClassModuleRunException("BAD_REQUEST", "classRuntimeState is required");
        }
        if (!request.getClassRuntimeState().isObject()) {
            throw new ClassModuleRunException("BAD_REQUEST", "classRuntimeState must be a JSON object");
        }
        ClassModuleRun run = classModuleRunRepository.findWithLockById(runId)
                .orElseThrow(() -> new ClassModuleRunException("RUN_NOT_FOUND", "Run not found"));
        JsonNode merged = deepMergeJsonNodes(emptyObjectIfNull(run.getClassRuntimeState()), request.getClassRuntimeState());
        run.setClassRuntimeState(merged);
        classModuleRunRepository.save(run);
        log.info("run_runtime_state_patched runId={}", runId);
        return buildStatusResponse(run);
    }

    /**
     * Dedicated runtime-state PATCH: either {@code { "stationFlowV1": {...}} } or wrapped
     * {@code { "classRuntimeState": { "stationFlowV1": {...} } } }.
     */
    @Transactional
    public ClassModuleRunStatusResponse patchRuntimeState(UUID runId, ObjectNode body) {
        if (body == null || body.isNull()) {
            throw new ClassModuleRunException("BAD_REQUEST", "Request body is required");
        }
        JsonNode patch;
        if (body.has("classRuntimeState")) {
            JsonNode wrapped = body.get("classRuntimeState");
            if (wrapped == null || wrapped.isNull()) {
                throw new ClassModuleRunException("BAD_REQUEST", "classRuntimeState must be a non-null JSON object");
            }
            if (!wrapped.isObject()) {
                throw new ClassModuleRunException("BAD_REQUEST", "classRuntimeState must be a JSON object");
            }
            patch = wrapped;
        } else if (!body.isObject()) {
            throw new ClassModuleRunException("BAD_REQUEST", "Body must be a JSON object");
        } else {
            patch = body;
        }
        ClassModuleRun run = classModuleRunRepository.findWithLockById(runId)
                .orElseThrow(() -> new ClassModuleRunException("RUN_NOT_FOUND", "Run not found"));
        JsonNode merged = deepMergeJsonNodes(emptyObjectIfNull(run.getClassRuntimeState()), patch);
        run.setClassRuntimeState(merged);
        classModuleRunRepository.save(run);
        log.info("run_runtime_state_patched path=runtime-state runId={}", runId);
        return buildStatusResponse(run);
    }

    private ClassModuleRunStatusResponse buildStatusResponse(ClassModuleRun run) {
        UUID runId = run.getId();
        List<ClassModuleRunStartResponse.GateItem> gates = classSectionGateRepository.findByRunIdOrderBySectionIndexAsc(runId)
                .stream()
                .map(g -> ClassModuleRunStartResponse.GateItem.builder()
                        .sectionId(g.getSectionId())
                        .sectionIndex(g.getSectionIndex())
                        .status(g.getStatus())
                        .build())
                .toList();

        JsonNode crs = run.getClassRuntimeState();
        if (crs != null && crs.isObject() && crs.size() == 0) {
            crs = null;
        }

        return ClassModuleRunStatusResponse.builder()
                .runId(run.getId())
                .schoolId(run.getSchoolId())
                .classId(run.getClassId())
                .moduleId(run.getModuleId())
                .status(run.getStatus())
                .activeSectionIndex(run.getActiveSectionIndex())
                .sectionGates(gates)
                .lastRollNumber(run.getLastRollNumber())
                .lastActivityId(run.getLastActivityId())
                .lastSectionId(run.getLastSectionId())
                .updatedAt(run.getLastInteractionAt())
                .classRuntimeState(crs)
                .build();
    }

    /** Deep-merge JSON objects; arrays and scalars from {@code patch} replace at that path. */
    private JsonNode deepMergeJsonNodes(JsonNode base, JsonNode patch) {
        if (patch == null || patch.isNull()) {
            return base == null ? JSON_NODES.objectNode() : base.deepCopy();
        }
        if (!patch.isObject()) {
            return patch.deepCopy();
        }
        ObjectNode out = (base != null && base.isObject())
                ? (ObjectNode) base.deepCopy()
                : JSON_NODES.objectNode();
        ObjectNode patchObj = (ObjectNode) patch;
        patchObj.properties().forEach(e -> {
            String key = e.getKey();
            JsonNode pv = e.getValue();
            JsonNode bv = out.get(key);
            if (bv != null && bv.isObject() && pv != null && pv.isObject()) {
                out.set(key, deepMergeJsonNodes(bv, pv));
            } else if (pv != null) {
                out.set(key, pv.deepCopy());
            }
        });
        return out;
    }

    private JsonNode emptyObjectIfNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return JSON_NODES.objectNode();
        }
        return node;
    }

    @Transactional(readOnly = true)
    public RunScope getRunScope(UUID runId) {
        ClassModuleRun run = getRunOrThrow(runId);
        return new RunScope(run.getSchoolId(), run.getClassId());
    }

    private ClassModuleRun getRunOrThrow(UUID runId) {
        return classModuleRunRepository.findById(runId)
                .orElseThrow(() -> new ClassModuleRunException("RUN_NOT_FOUND", "Run not found"));
    }

    private String normalizeCompletionRule(String rule) {
        if (rule == null || rule.isBlank()) return "all_students";
        String lower = rule.trim().toLowerCase(Locale.ROOT);
        if (!"all_students".equals(lower) && !"threshold".equals(lower)) {
            throw new ClassModuleRunException("BAD_REQUEST", "completionRule must be all_students or threshold");
        }
        return lower;
    }

    private void refreshGateAndRunState(ClassModuleRun run, ClassSectionGate gate) {
        int totalStudents = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(
                run.getSchoolId(), run.getClassId(), USER_TYPE_STUDENT).size();
        long completedStudents = studentSectionProgressRepository.countByRunIdAndSectionIdAndStatus(
                run.getId(), gate.getSectionId(), STUDENT_COMPLETED);

        int completionPct = totalStudents == 0 ? 0 : (int) Math.floor((completedStudents * 100.0) / totalStudents);
        boolean gateSatisfied = "all_students".equals(gate.getCompletionRule())
                ? totalStudents > 0 && completedStudents == totalStudents
                : completionPct >= gate.getCompletionTarget();

        if (gateSatisfied && !GATE_COMPLETED.equals(gate.getStatus())) {
            gate.setStatus(GATE_COMPLETED);
            gate.setCompletedAt(Instant.now());
            classSectionGateRepository.save(gate);
            log.info("class_section_completed runId={} sectionId={}", run.getId(), gate.getSectionId());

            Optional<ClassSectionGate> nextGateOpt = classSectionGateRepository.findWithLockByRunIdAndSectionIndex(run.getId(), gate.getSectionIndex() + 1);
            if (nextGateOpt.isPresent()) {
                ClassSectionGate nextGate = nextGateOpt.get();
                if (GATE_LOCKED.equals(nextGate.getStatus())) {
                    nextGate.setStatus(GATE_UNLOCKED);
                    nextGate.setUnlockedAt(Instant.now());
                    classSectionGateRepository.save(nextGate);
                    run.setActiveSectionIndex(nextGate.getSectionIndex());
                    classModuleRunRepository.save(run);
                    log.info("class_section_unlocked runId={} sectionId={}", run.getId(), nextGate.getSectionId());
                }
            } else {
                run.setStatus(RUN_COMPLETED);
                run.setCompletedAt(Instant.now());
                classModuleRunRepository.save(run);
                log.info("run_completed runId={} classId={} moduleId={}", run.getId(), run.getClassId(), run.getModuleId());
            }
        }
    }

    private ClassModuleRunStartResponse toStartResponse(ClassModuleRun run, List<ClassSectionGate> gates) {
        String activeSectionId = null;
        if (run.getActiveSectionIndex() >= 0 && run.getActiveSectionIndex() < run.getSectionOrder().size()) {
            activeSectionId = run.getSectionOrder().get(run.getActiveSectionIndex());
        }
        List<ClassModuleRunStartResponse.GateItem> gateItems = gates.stream()
                .map(g -> ClassModuleRunStartResponse.GateItem.builder()
                        .sectionId(g.getSectionId())
                        .sectionIndex(g.getSectionIndex())
                        .status(g.getStatus())
                        .build())
                .toList();

        return ClassModuleRunStartResponse.builder()
                .runId(run.getId())
                .status(run.getStatus())
                .activeSectionIndex(run.getActiveSectionIndex())
                .activeSectionId(activeSectionId)
                .sectionGateStatus(gateItems)
                .build();
    }

    private void validateSectionIds(List<String> sectionIds) {
        Set<String> seen = new HashSet<>();
        for (String sectionId : sectionIds) {
            if (sectionId == null || sectionId.isBlank()) {
                throw new ClassModuleRunException("BAD_REQUEST", "sectionIds contains blank section");
            }
            if (!seen.add(sectionId)) {
                throw new ClassModuleRunException("BAD_REQUEST", "sectionIds contains duplicates");
            }
        }
    }

    private void validateSectionActivities(List<String> sectionIds, Map<String, List<String>> sectionActivities) {
        if (sectionActivities == null || sectionActivities.isEmpty()) {
            throw new ClassModuleRunException("BAD_REQUEST", "sectionActivities is required: map each sectionId to an ordered list of activity slugs");
        }
        if (!sectionActivities.keySet().equals(new HashSet<>(sectionIds))) {
            throw new ClassModuleRunException("BAD_REQUEST", "sectionActivities keys must exactly match sectionIds (same set, one entry per section)");
        }
        for (String sectionId : sectionIds) {
            List<String> acts = sectionActivities.get(sectionId);
            if (acts == null || acts.isEmpty()) {
                throw new ClassModuleRunException("BAD_REQUEST", "sectionActivities[" + sectionId + "] must be a non-empty ordered list of activity slugs");
            }
            for (String slug : acts) {
                if (slug == null || slug.isBlank()) {
                    throw new ClassModuleRunException("BAD_REQUEST", "sectionActivities[" + sectionId + "] contains blank activity slug");
                }
                if (slug.equals(sectionId)) {
                    throw new ClassModuleRunException("BAD_REQUEST", "activity slug must not equal section id: " + sectionId);
                }
            }
        }
    }

    private List<String> curriculumForSectionOrThrow(ClassModuleRun run, String sectionId) {
        Map<String, List<String>> map = run.getSectionActivities();
        if (map == null || map.isEmpty()) {
            throw new ClassModuleRunException(
                    "CURRICULUM_MISSING",
                    "This run has no sectionActivities; create a new run with POST /start including sectionActivities");
        }
        List<String> list = map.get(sectionId);
        if (list == null || list.isEmpty()) {
            throw new ClassModuleRunException("CURRICULUM_MISSING", "No activity slugs defined for section " + sectionId);
        }
        return list;
    }

    /**
     * When allowed=true, activityId is always a real curriculum slug from sectionActivities (never the section id).
     */
    private RollResolveNextResponse buildAllowedResolveResponse(ClassModuleRun run, ClassSectionGate gate, StudentSectionProgress progress) {
        List<String> curriculum = curriculumForSectionOrThrow(run, gate.getSectionId());
        String studentStatus = progress != null && progress.getStatus() != null ? progress.getStatus() : STUDENT_NOT_STARTED;

        String activityId;
        Integer challengeIndex = null;

        if (progress == null || STUDENT_NOT_STARTED.equals(studentStatus)) {
            activityId = curriculum.get(0);
        } else if (STUDENT_IN_PROGRESS.equals(studentStatus)) {
            String last = progress.getLastActivityId();
            if (last != null && !last.isBlank() && curriculum.contains(last)) {
                activityId = last;
                Integer ci = progress.getLastChallengeIndex();
                challengeIndex = ci != null ? ci + 1 : 0;
            } else {
                log.warn("resolve_next_curriculum_fallback runId={} sectionId={} lastActivityId={} — using first activity in section",
                        run.getId(), gate.getSectionId(), last);
                activityId = curriculum.get(0);
            }
        } else {
            activityId = curriculum.get(0);
        }

        return RollResolveNextResponse.builder()
                .allowed(true)
                .reason("ok")
                .sectionId(gate.getSectionId())
                .sectionIndex(gate.getSectionIndex())
                .activityId(activityId)
                .challengeIndex(challengeIndex)
                .studentSectionStatus(studentStatus)
                .classSectionStatus(gate.getStatus())
                .build();
    }
}
