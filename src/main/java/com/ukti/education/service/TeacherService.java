package com.ukti.education.service;

import com.ukti.education.dto.AddTeacherRequest;
import com.ukti.education.dto.TeacherResponse;
import com.ukti.education.entity.School;
import com.ukti.education.entity.SchoolClass;
import com.ukti.education.entity.Teacher;
import com.ukti.education.entity.TeacherClass;
import com.ukti.education.repository.SchoolClassRepository;
import com.ukti.education.repository.SchoolRepository;
import com.ukti.education.repository.TeacherClassRepository;
import com.ukti.education.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeacherResponse addTeacher(UUID schoolId, AddTeacherRequest request) {
        if (teacherRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
            throw new IllegalArgumentException("Teacher with email '" + request.getEmail() + "' already exists in this school");
        }

        Teacher teacher = Teacher.builder()
                .schoolId(schoolId)
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName() != null ? request.getName().trim() : null)
                .build();
        teacher = teacherRepository.save(teacher);
        log.info("TeacherService: Added teacher {} to school {}", teacher.getEmail(), schoolId);
        return toSimpleResponse(teacher);
    }

    public List<TeacherResponse> listTeachers(UUID schoolId) {
        List<Teacher> teachers = teacherRepository.findBySchoolIdOrderByEmail(schoolId);
        return teachers.stream().map(this::toSimpleResponse).collect(Collectors.toList());
    }

    public Optional<TeacherResponse> validateAndGetTeacher(String email, String password, UUID schoolId) {
        Optional<Teacher> teacherOpt;
        if (schoolId != null) {
            teacherOpt = teacherRepository.findBySchoolIdAndEmail(schoolId, email.trim().toLowerCase());
        } else {
            List<Teacher> teachers = teacherRepository.findByEmail(email.trim().toLowerCase());
            teacherOpt = teachers.isEmpty() ? Optional.empty() : Optional.of(teachers.get(0));
            if (teachers.size() > 1) {
                log.warn("Multiple teachers with email {}, schoolId required", email);
                return Optional.empty();
            }
        }

        if (teacherOpt.isEmpty()) return Optional.empty();
        Teacher teacher = teacherOpt.get();
        if (!passwordEncoder.matches(password, teacher.getPasswordHash())) return Optional.empty();

        return Optional.of(toFullResponse(teacher));
    }

    public Optional<Teacher> getTeacherById(UUID teacherId) {
        return teacherRepository.findById(teacherId);
    }

    @Transactional
    public void assignTeacherToClass(UUID teacherId, UUID classId) {
        if (!teacherClassRepository.existsById(new TeacherClass.TeacherClassId(teacherId, classId))) {
            teacherClassRepository.save(TeacherClass.builder()
                    .teacherId(teacherId)
                    .classId(classId)
                    .isMainTeacher(true)
                    .build());
        }
    }

    private TeacherResponse toSimpleResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId().toString())
                .email(teacher.getEmail())
                .name(teacher.getName())
                .build();
    }

    private TeacherResponse toFullResponse(Teacher teacher) {
        String schoolName = schoolRepository.findById(teacher.getSchoolId())
                .map(School::getName)
                .orElse(null);

        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacher.getId());
        List<TeacherResponse.ClassSummary> classes = new ArrayList<>();
        for (TeacherClass tc : assignments) {
            schoolClassRepository.findById(tc.getClassId()).ifPresent(c ->
                    classes.add(TeacherResponse.ClassSummary.builder()
                            .id(c.getId().toString())
                            .name(c.getName())
                            .build()));
        }

        return TeacherResponse.builder()
                .id(teacher.getId().toString())
                .email(teacher.getEmail())
                .name(teacher.getName())
                .schoolId(teacher.getSchoolId().toString())
                .schoolName(schoolName)
                .classes(classes)
                .build();
    }
}
