package com.ukti.education.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukti.education.dto.ClassModuleActivityCompleteRequest;
import com.ukti.education.dto.ClassModuleRunPatchRequest;
import com.ukti.education.dto.ClassModuleRunStartRequest;
import com.ukti.education.entity.*;
import com.ukti.education.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassModuleRunServiceTest {

    @Mock
    private ClassModuleRunRepository classModuleRunRepository;
    @Mock
    private ClassSectionGateRepository classSectionGateRepository;
    @Mock
    private StudentSectionProgressRepository studentSectionProgressRepository;
    @Mock
    private StudentActivityProgressRepository studentActivityProgressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;

    @InjectMocks
    private ClassModuleRunService service;

    private UUID runId;
    private UUID schoolId;
    private UUID classId;
    private UUID studentId;
    private User student;
    private ClassModuleRun run;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        classId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        student = User.builder()
                .id(studentId)
                .schoolUuid(schoolId)
                .classId(classId)
                .rollNumber("17")
                .userType("student")
                .build();

        run = ClassModuleRun.builder()
                .id(runId)
                .schoolId(schoolId)
                .classId(classId)
                .moduleId("M1")
                .status("active")
                .activeSectionIndex(0)
                .sectionOrder(List.of("section-a", "section-b"))
                .sectionActivities(Map.of(
                        "section-a", List.of("playgroup-colour-1", "playgroup-letter-1"),
                        "section-b", List.of("playgroup-colour-2")))
                .completionRule("all_students")
                .completionTarget(100)
                .build();
    }

    @Test
    void startRunRejectsDuplicateSectionIds() {
        ClassModuleRunStartRequest request = new ClassModuleRunStartRequest();
        request.setSchoolId(schoolId);
        request.setClassId(classId);
        request.setModuleId("M1");
        request.setSectionIds(List.of("section-a", "section-a"));

        ClassModuleRunException ex = assertThrows(ClassModuleRunException.class, () -> service.startOrResume(request, null));
        assertEquals("BAD_REQUEST", ex.getCode());
        assertTrue(ex.getMessage().contains("duplicates"));
    }

    @Test
    void completeActivityRejectsOutOfOrderSectionSubmission() {
        when(classModuleRunRepository.findWithLockById(runId)).thenReturn(Optional.of(run));
        when(classModuleRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(schoolId, classId, "17", "student"))
                .thenReturn(Optional.of(student));
        when(classSectionGateRepository.findWithLockByRunIdAndSectionId(runId, "section-b"))
                .thenReturn(Optional.of(ClassSectionGate.builder()
                        .runId(runId).sectionId("section-b").sectionIndex(1).status("unlocked")
                        .completionRule("all_students").completionTarget(100).build()));

        when(classSectionGateRepository.findByRunIdOrderBySectionIndexAsc(runId)).thenReturn(List.of(
                ClassSectionGate.builder().runId(runId).sectionId("section-a").sectionIndex(0).status("unlocked").build(),
                ClassSectionGate.builder().runId(runId).sectionId("section-b").sectionIndex(1).status("unlocked").build()
        ));
        when(studentSectionProgressRepository.findByRunIdAndRollNumberOrderBySectionIdAsc(runId, "17")).thenReturn(List.of(
                StudentSectionProgress.builder().runId(runId).rollNumber("17").sectionId("section-a").status("not_started").build(),
                StudentSectionProgress.builder().runId(runId).rollNumber("17").sectionId("section-b").status("not_started").build()
        ));

        ClassModuleActivityCompleteRequest request = new ClassModuleActivityCompleteRequest();
        request.setSectionId("section-b");
        request.setResult("passed");
        request.setMetadata(Map.of("test", true));

        ClassModuleRunException ex = assertThrows(
                ClassModuleRunException.class,
                () -> service.completeActivity(runId, "17", "activity-2", null, request)
        );
        assertEquals("INVALID_SECTION_FOR_ROLL", ex.getCode());
        verify(studentActivityProgressRepository, never()).save(any());
    }

    @Test
    void completeActivityDetectsIdempotencyConflict() {
        when(classModuleRunRepository.findWithLockById(runId)).thenReturn(Optional.of(run));
        when(userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(schoolId, classId, "17", "student"))
                .thenReturn(Optional.of(student));
        when(studentActivityProgressRepository.findByRunIdAndIdempotencyKey(runId, "idem-1"))
                .thenReturn(Optional.of(StudentActivityProgress.builder()
                        .runId(runId)
                        .rollNumber("17")
                        .sectionId("section-a")
                        .activityId("activity-1")
                        .challengeIndex(0)
                        .build()));

        ClassModuleActivityCompleteRequest request = new ClassModuleActivityCompleteRequest();
        request.setSectionId("section-a");
        request.setResult("passed");
        request.setChallengeIndex(1); // mismatch vs existing challengeIndex=0

        ClassModuleRunException ex = assertThrows(
                ClassModuleRunException.class,
                () -> service.completeActivity(runId, "17", "activity-1", "idem-1", request)
        );
        assertEquals("IDEMPOTENCY_CONFLICT", ex.getCode());
    }

    @Test
    void completeActivityUnlocksNextSectionWhenGateSatisfied() {
        when(classModuleRunRepository.findWithLockById(runId)).thenReturn(Optional.of(run));
        when(classModuleRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(userRepository.findBySchoolUuidAndClassIdAndRollNumberAndUserType(schoolId, classId, "17", "student"))
                .thenReturn(Optional.of(student));

        ClassSectionGate currentGate = ClassSectionGate.builder()
                .runId(runId).sectionId("section-a").sectionIndex(0).status("unlocked")
                .completionRule("all_students").completionTarget(100).build();
        ClassSectionGate nextGate = ClassSectionGate.builder()
                .runId(runId).sectionId("section-b").sectionIndex(1).status("locked")
                .completionRule("all_students").completionTarget(100).build();

        when(classSectionGateRepository.findWithLockByRunIdAndSectionId(runId, "section-a")).thenReturn(Optional.of(currentGate));
        when(studentSectionProgressRepository.findWithLockByRunIdAndRollNumberAndSectionId(runId, "17", "section-a"))
                .thenReturn(Optional.of(StudentSectionProgress.builder()
                        .runId(runId).rollNumber("17").sectionId("section-a").status("in_progress").attemptCount(0).build()));
        when(userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(schoolId, classId, "student"))
                .thenReturn(List.of(student));
        when(studentSectionProgressRepository.countByRunIdAndSectionIdAndStatus(runId, "section-a", "completed"))
                .thenReturn(1L);
        when(classSectionGateRepository.findWithLockByRunIdAndSectionIndex(runId, 1)).thenReturn(Optional.of(nextGate));

        // resolveNext called before and after complete
        when(classSectionGateRepository.findByRunIdOrderBySectionIndexAsc(runId))
                .thenReturn(List.of(
                        ClassSectionGate.builder().runId(runId).sectionId("section-a").sectionIndex(0).status("unlocked").build(),
                        ClassSectionGate.builder().runId(runId).sectionId("section-b").sectionIndex(1).status("locked").build()
                ))
                .thenReturn(List.of(
                        ClassSectionGate.builder().runId(runId).sectionId("section-a").sectionIndex(0).status("completed").build(),
                        ClassSectionGate.builder().runId(runId).sectionId("section-b").sectionIndex(1).status("unlocked").build()
                ));
        when(studentSectionProgressRepository.findByRunIdAndRollNumberOrderBySectionIdAsc(runId, "17"))
                .thenReturn(List.of(
                        StudentSectionProgress.builder().runId(runId).rollNumber("17").sectionId("section-a").status("in_progress").build(),
                        StudentSectionProgress.builder().runId(runId).rollNumber("17").sectionId("section-b").status("not_started").build()
                ))
                .thenReturn(List.of(
                        StudentSectionProgress.builder().runId(runId).rollNumber("17").sectionId("section-a").status("completed").build(),
                        StudentSectionProgress.builder().runId(runId).rollNumber("17").sectionId("section-b").status("not_started").build()
                ));

        ClassModuleActivityCompleteRequest request = new ClassModuleActivityCompleteRequest();
        request.setSectionId("section-a");
        request.setResult("passed");
        request.setChallengeIndex(0);

        service.completeActivity(runId, "17", "activity-1", "idem-2", request);

        ArgumentCaptor<ClassSectionGate> gateCaptor = ArgumentCaptor.forClass(ClassSectionGate.class);
        verify(classSectionGateRepository, atLeast(2)).save(gateCaptor.capture());
        List<ClassSectionGate> savedGates = gateCaptor.getAllValues();
        assertTrue(savedGates.stream().anyMatch(g -> "section-a".equals(g.getSectionId()) && "completed".equals(g.getStatus())));
        assertTrue(savedGates.stream().anyMatch(g -> "section-b".equals(g.getSectionId()) && "unlocked".equals(g.getStatus())));
        verify(classModuleRunRepository, atLeastOnce()).save(eq(run));
    }

    @Test
    void patchRunDeepMergesStationFlowV1() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode existing = om.readTree(
                "{\"stationFlowV1\":{\"v\":1,\"rowIndex\":0,\"rollsDoneForStep\":[\"1\"]}}");
        run.setClassRuntimeState(existing);
        when(classModuleRunRepository.findWithLockById(runId)).thenReturn(Optional.of(run));
        when(classSectionGateRepository.findByRunIdOrderBySectionIndexAsc(runId)).thenReturn(List.of());

        ClassModuleRunPatchRequest req = new ClassModuleRunPatchRequest();
        Map<String, Object> patch = om.readValue(
                "{\"stationFlowV1\":{\"challengeIndex\":2,\"rollsDoneForStep\":[\"1\",\"3\"]}}",
                new TypeReference<Map<String, Object>>() {});
        req.setClassRuntimeState(patch);

        service.patchRun(runId, req);

        ArgumentCaptor<ClassModuleRun> cap = ArgumentCaptor.forClass(ClassModuleRun.class);
        verify(classModuleRunRepository).save(cap.capture());
        JsonNode merged = cap.getValue().getClassRuntimeState();
        assertEquals(2, merged.get("stationFlowV1").get("challengeIndex").asInt());
        assertEquals(0, merged.get("stationFlowV1").get("rowIndex").asInt());
        assertEquals(2, merged.get("stationFlowV1").get("rollsDoneForStep").size());
    }
}
