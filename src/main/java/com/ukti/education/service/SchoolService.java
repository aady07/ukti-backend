package com.ukti.education.service;

import com.ukti.education.dto.AddStudentsRequest;
import com.ukti.education.dto.AddStudentsResponse;
import com.ukti.education.dto.AddTeacherRequest;
import com.ukti.education.dto.ClassCreateRequest;
import com.ukti.education.dto.ClassLeaderboardResponse;
import com.ukti.education.dto.ClassResponse;
import com.ukti.education.dto.LastActivityResponse;
import com.ukti.education.dto.TeacherResponse;
import com.ukti.education.dto.SchoolProgressOverviewResponse;
import com.ukti.education.dto.StudentResponse;
import com.ukti.education.entity.SchoolClass;
import com.ukti.education.entity.User;
import com.ukti.education.repository.SchoolClassRepository;
import com.ukti.education.repository.UserActivityProgressRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolService {

    private static final String USER_TYPE_STUDENT = "student";

    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final UserActivityProgressRepository userActivityProgressRepository;
    private final TeacherService teacherService;
    private final com.ukti.education.repository.TeacherRepository teacherRepository;
    private final com.ukti.education.repository.TeacherClassRepository teacherClassRepository;

    public List<ClassResponse> listClasses(UUID schoolId) {
        List<SchoolClass> classes = schoolClassRepository.findBySchoolIdOrderByName(schoolId);
        List<ClassResponse> result = new ArrayList<>();
        for (SchoolClass c : classes) {
            long count = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(schoolId, c.getId(), USER_TYPE_STUDENT).size();
            String teacherId = null;
            String teacherName = null;
            List<com.ukti.education.entity.TeacherClass> assignments = teacherClassRepository.findByClassId(c.getId());
            if (!assignments.isEmpty()) {
                com.ukti.education.entity.TeacherClass tc = assignments.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsMainTeacher()))
                        .findFirst()
                        .orElse(assignments.get(0));
                var teacherOpt = teacherRepository.findById(tc.getTeacherId());
                if (teacherOpt.isPresent()) {
                    var t = teacherOpt.get();
                    teacherId = t.getId().toString();
                    teacherName = t.getName() != null ? t.getName() : t.getEmail();
                }
            }
            result.add(ClassResponse.builder()
                    .id(c.getId().toString())
                    .name(c.getName())
                    .studentCount((int) count)
                    .teacherId(teacherId)
                    .teacherName(teacherName)
                    .build());
        }
        return result;
    }

    public ClassResponse createClass(UUID schoolId, ClassCreateRequest request) {
        if (schoolClassRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new IllegalArgumentException("Class with name '" + request.getName() + "' already exists");
        }
        SchoolClass schoolClass = SchoolClass.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .build();
        schoolClass = schoolClassRepository.save(schoolClass);

        if (request.getTeacherId() != null && !request.getTeacherId().isBlank()) {
            try {
                UUID teacherId = UUID.fromString(request.getTeacherId().trim());
                if (teacherRepository.findById(teacherId).filter(t -> t.getSchoolId().equals(schoolId)).isPresent()) {
                    teacherClassRepository.save(com.ukti.education.entity.TeacherClass.builder()
                            .teacherId(teacherId)
                            .classId(schoolClass.getId())
                            .isMainTeacher(true)
                            .build());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return ClassResponse.builder()
                .id(schoolClass.getId().toString())
                .name(schoolClass.getName())
                .studentCount(0)
                .build();
    }

    public TeacherResponse addTeacher(UUID schoolId, AddTeacherRequest request) {
        return teacherService.addTeacher(schoolId, request);
    }

    @Transactional
    public void assignTeacherToClass(UUID schoolId, UUID classId, String teacherIdStr) {
        if (!schoolClassRepository.findById(classId).filter(c -> c.getSchoolId().equals(schoolId)).isPresent()) {
            throw new IllegalArgumentException("Class not found or does not belong to school");
        }
        if (teacherIdStr == null || teacherIdStr.isBlank()) {
            return;  // No teacher to assign
        }
        try {
            UUID teacherId = UUID.fromString(teacherIdStr.trim());
            if (!teacherRepository.findById(teacherId).filter(t -> t.getSchoolId().equals(schoolId)).isPresent()) {
                throw new IllegalArgumentException("Teacher not found or does not belong to school");
            }
            teacherService.assignTeacherToClass(teacherId, classId);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public List<TeacherResponse> listTeachers(UUID schoolId) {
        return teacherService.listTeachers(schoolId);
    }

    @Transactional
    public AddStudentsResponse addStudents(UUID schoolId, UUID classId, AddStudentsRequest request) {
        List<StudentResponse> created = new ArrayList<>();
        for (AddStudentsRequest.StudentInput input : request.getStudents()) {
            if (userRepository.existsBySchoolUuidAndClassIdAndRollNumberAndUserType(schoolId, classId, input.getRollNumber(), USER_TYPE_STUDENT)) {
                log.warn("Skipping duplicate roll number {} in class {}", input.getRollNumber(), classId);
                continue;
            }
            User student = User.builder()
                    .userType(USER_TYPE_STUDENT)
                    .schoolUuid(schoolId)
                    .classId(classId)
                    .rollNumber(input.getRollNumber())
                    .displayName(input.getName())
                    .cognitoSub(null)
                    .email(null)
                    .passwordHash("STUDENT")
                    .build();
            student = userRepository.save(student);
            created.add(StudentResponse.builder()
                    .id(student.getId().toString())
                    .rollNumber(student.getRollNumber())
                    .name(student.getDisplayName())
                    .build());
        }
        return AddStudentsResponse.builder().created(created).build();
    }

    public Set<UUID> getTeacherClassIds(UUID teacherId) {
        return teacherClassRepository.findByTeacherId(teacherId).stream()
                .map(com.ukti.education.entity.TeacherClass::getClassId)
                .collect(Collectors.toSet());
    }

    /**
     * Verifies the class exists, belongs to the school, and (if teacher) the teacher is assigned to it.
     */
    public boolean canAccessClass(UUID schoolId, UUID classId, UUID teacherIdOrNull) {
        Optional<SchoolClass> cls = schoolClassRepository.findById(classId);
        if (cls.isEmpty() || !cls.get().getSchoolId().equals(schoolId)) return false;
        if (teacherIdOrNull != null && !teacherClassRepository.existsByTeacherIdAndClassId(teacherIdOrNull, classId)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the most recent activity completed by any student in this class.
     * Empty if no activity in class.
     */
    public Optional<LastActivityResponse> getLastActivity(UUID schoolId, UUID classId) {
        List<com.ukti.education.entity.UserActivityProgress> latest = userActivityProgressRepository
                .findLatestBySchoolAndClass(schoolId, classId, PageRequest.of(0, 1));
        if (latest.isEmpty()) return Optional.empty();

        com.ukti.education.entity.UserActivityProgress p = latest.get(0);
        User student = p.getUser();
        return Optional.of(LastActivityResponse.builder()
                .lastActivity(LastActivityResponse.LastActivity.builder()
                        .activityId(p.getActivitySlug())
                        .activityLabel(humanizeActivitySlug(p.getActivitySlug()))
                        .unitId(p.getUnitSlug())
                        .completedAt(p.getCompletedAt())
                        .rollNumber(student != null ? student.getRollNumber() : null)
                        .studentName(student != null ? student.getDisplayName() : null)
                        .build())
                .build());
    }

    /**
     * Returns top N students by completed activities count in this class.
     */
    public ClassLeaderboardResponse getClassLeaderboard(UUID schoolId, UUID classId, int limit) {
        List<User> students = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(schoolId, classId, USER_TYPE_STUDENT);
        List<ClassLeaderboardResponse.LeaderboardEntry> leaderboard = new ArrayList<>();
        List<User> sorted = students.stream()
                .sorted((a, b) -> Long.compare(
                        userActivityProgressRepository.countByUserId(b.getId()),
                        userActivityProgressRepository.countByUserId(a.getId())))
                .limit(Math.max(1, limit))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            User s = sorted.get(i);
            long count = userActivityProgressRepository.countByUserId(s.getId());
            leaderboard.add(ClassLeaderboardResponse.LeaderboardEntry.builder()
                    .rank(i + 1)
                    .rollNumber(s.getRollNumber())
                    .name(s.getDisplayName())
                    .completedCount((int) count)
                    .build());
        }
        return ClassLeaderboardResponse.builder().leaderboard(leaderboard).build();
    }

    private static String humanizeActivitySlug(String slug) {
        if (slug == null || slug.isBlank()) return slug;
        return java.util.Arrays.stream(slug.split("-"))
                .map(word -> word.isEmpty() ? "" : word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public List<StudentResponse> listStudents(UUID schoolId, UUID classId) {
        List<User> students = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(schoolId, classId, USER_TYPE_STUDENT);
        List<StudentResponse> result = new ArrayList<>();
        for (User s : students) {
            result.add(StudentResponse.builder()
                    .id(s.getId().toString())
                    .rollNumber(s.getRollNumber())
                    .name(s.getDisplayName())
                    .build());
        }
        return result;
    }

    /**
     * Get progress overview for school admin dashboard.
     * Returns admin's progress + all students' progress by class in one call.
     * When classIdsToInclude is not null (teacher), only includes those classes.
     */
    public SchoolProgressOverviewResponse getProgressOverview(UUID schoolId, UUID adminUserId, Integer totalActivities, Set<UUID> classIdsToInclude) {
        int total = totalActivities != null && totalActivities > 0 ? totalActivities : 0;

        long adminCompleted = adminUserId != null ? userActivityProgressRepository.countByUserId(adminUserId) : 0;
        List<SchoolProgressOverviewResponse.UnitProgressSummary> adminUnits = adminUserId != null ? getUnitProgressForUser(adminUserId) : List.of();
        int adminPercent = total > 0 ? (int) Math.round(100.0 * adminCompleted / total) : 0;

        SchoolProgressOverviewResponse.AdminProgressSummary adminProgress = SchoolProgressOverviewResponse.AdminProgressSummary.builder()
                .completedCount((int) adminCompleted)
                .totalCount(total)
                .percent(adminPercent)
                .units(adminUnits)
                .build();

        List<SchoolClass> classes = schoolClassRepository.findBySchoolIdOrderByName(schoolId);
        if (classIdsToInclude != null && !classIdsToInclude.isEmpty()) {
            classes = classes.stream().filter(c -> classIdsToInclude.contains(c.getId())).toList();
        }
        List<SchoolProgressOverviewResponse.ClassProgressSummary> classSummaries = new ArrayList<>();

        for (SchoolClass c : classes) {
            List<User> students = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(schoolId, c.getId(), USER_TYPE_STUDENT);
            List<SchoolProgressOverviewResponse.StudentProgressSummary> studentSummaries = new ArrayList<>();

            for (User s : students) {
                long completed = userActivityProgressRepository.countByUserId(s.getId());
                int percent = total > 0 ? (int) Math.round(100.0 * completed / total) : 0;
                studentSummaries.add(SchoolProgressOverviewResponse.StudentProgressSummary.builder()
                        .studentId(s.getId().toString())
                        .rollNumber(s.getRollNumber())
                        .name(s.getDisplayName())
                        .completedCount((int) completed)
                        .totalCount(total)
                        .percent(percent)
                        .build());
            }

            classSummaries.add(SchoolProgressOverviewResponse.ClassProgressSummary.builder()
                    .classId(c.getId().toString())
                    .className(c.getName())
                    .students(studentSummaries)
                    .build());
        }

        return SchoolProgressOverviewResponse.builder()
                .adminProgress(adminProgress)
                .classes(classSummaries)
                .build();
    }

    private List<SchoolProgressOverviewResponse.UnitProgressSummary> getUnitProgressForUser(UUID userId) {
        List<com.ukti.education.entity.UserActivityProgress> all = userActivityProgressRepository.findByUserIdOrderByCompletedAtDesc(userId);
        Map<String, Long> byUnit = all.stream()
                .collect(Collectors.groupingBy(com.ukti.education.entity.UserActivityProgress::getUnitSlug, Collectors.counting()));
        return byUnit.entrySet().stream()
                .map(e -> SchoolProgressOverviewResponse.UnitProgressSummary.builder()
                        .unitId(e.getKey())
                        .completedCount(e.getValue().intValue())
                        .totalCount(0)
                        .build())
                .collect(Collectors.toList());
    }
}
