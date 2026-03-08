package com.ukti.education.service;

import com.ukti.education.dto.*;
import com.ukti.education.entity.ExperientialStat;
import com.ukti.education.entity.ModuleCompletion;
import com.ukti.education.entity.TaskCompletion;
import com.ukti.education.entity.User;
import com.ukti.education.repository.ExperientialStatRepository;
import com.ukti.education.repository.ModuleCompletionRepository;
import com.ukti.education.repository.TaskCompletionRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final ModuleCompletionRepository moduleCompletionRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final ExperientialStatRepository experientialStatRepository;
    private final UserRepository userRepository;

    @Transactional
    public ModuleProgressResponse completeModule(UUID userId, String moduleId) {
        User user = userRepository.findById(userId).orElseThrow();
        Optional<ModuleCompletion> existing = moduleCompletionRepository.findByUserIdAndModuleId(userId, moduleId);

        ModuleCompletion mc;
        if (existing.isPresent()) {
            mc = existing.get();
            log.info("ProgressService: Module already completed userId={}, moduleId={}", userId, moduleId);
        } else {
            mc = ModuleCompletion.builder()
                    .user(user)
                    .moduleId(moduleId)
                    .completedAt(Instant.now())
                    .build();
            mc = moduleCompletionRepository.save(mc);
            log.info("ProgressService: Module completed userId={}, moduleId={}", userId, moduleId);
        }

        List<TaskCompletion> tasks = taskCompletionRepository.findByUserIdAndModuleId(userId, moduleId);
        return ModuleProgressResponse.builder()
                .moduleId(moduleId)
                .completed(true)
                .completedAt(mc.getCompletedAt())
                .tasks(tasks.stream()
                        .map(t -> ModuleProgressResponse.TaskProgressItem.builder()
                                .taskId(t.getTaskId())
                                .taskType(t.getTaskType())
                                .completedAt(t.getCompletedAt())
                                .metadata(t.getMetadata())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public Optional<ModuleProgressResponse> getModuleProgress(UUID userId, String moduleId) {
        Optional<ModuleCompletion> mc = moduleCompletionRepository.findByUserIdAndModuleId(userId, moduleId);
        List<TaskCompletion> tasks = taskCompletionRepository.findByUserIdAndModuleId(userId, moduleId);

        return Optional.of(ModuleProgressResponse.builder()
                .moduleId(moduleId)
                .completed(mc.isPresent())
                .completedAt(mc.map(ModuleCompletion::getCompletedAt).orElse(null))
                .tasks(tasks.stream()
                        .map(t -> ModuleProgressResponse.TaskProgressItem.builder()
                                .taskId(t.getTaskId())
                                .taskType(t.getTaskType())
                                .completedAt(t.getCompletedAt())
                                .metadata(t.getMetadata())
                                .build())
                        .collect(Collectors.toList()))
                .build());
    }

    public UserProgressResponse getUserProgress(UUID userId) {
        List<ModuleCompletion> modules = moduleCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId);
        List<TaskCompletion> tasks = taskCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId);
        List<ExperientialStat> stats = experientialStatRepository.findByUserId(userId);

        return UserProgressResponse.builder()
                .modules(modules.stream()
                        .map(m -> UserProgressResponse.ModuleProgressItem.builder()
                                .moduleId(m.getModuleId())
                                .completedAt(m.getCompletedAt().toString())
                                .build())
                        .collect(Collectors.toList()))
                .tasks(tasks.stream()
                        .map(t -> UserProgressResponse.TaskProgressItem.builder()
                                .moduleId(t.getModuleId())
                                .taskId(t.getTaskId())
                                .taskType(t.getTaskType())
                                .completedAt(t.getCompletedAt().toString())
                                .metadata(t.getMetadata())
                                .build())
                        .collect(Collectors.toList()))
                .experientialStats(stats.stream()
                        .map(s -> UserProgressResponse.ExperientialStatItem.builder()
                                .moduleId(s.getModuleId())
                                .statType(s.getStatType())
                                .count(s.getCount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public TaskCompletionResponse recordTaskCompletion(UUID userId, TaskCompletionRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        Optional<TaskCompletion> existing = taskCompletionRepository.findByUserIdAndModuleIdAndTaskId(
                userId, request.getModuleId(), request.getTaskId());

        TaskCompletion tc;
        if (existing.isPresent()) {
            tc = existing.get();
            if (request.getTaskType() != null) tc.setTaskType(request.getTaskType());
            if (request.getMetadata() != null) tc.setMetadata(request.getMetadata());
            tc = taskCompletionRepository.save(tc);
            log.info("ProgressService: Task updated userId={}, moduleId={}, taskId={}", userId, request.getModuleId(), request.getTaskId());
        } else {
            tc = TaskCompletion.builder()
                    .user(user)
                    .moduleId(request.getModuleId())
                    .taskId(request.getTaskId())
                    .taskType(request.getTaskType())
                    .metadata(request.getMetadata())
                    .completedAt(Instant.now())
                    .build();
            tc = taskCompletionRepository.save(tc);
            log.info("ProgressService: Task completed userId={}, moduleId={}, taskId={}", userId, request.getModuleId(), request.getTaskId());
        }

        return TaskCompletionResponse.builder()
                .taskId(tc.getTaskId())
                .completedAt(tc.getCompletedAt())
                .build();
    }

    @Transactional
    public ExperientialStatResponse updateExperientialStat(UUID userId, String moduleId, ExperientialStatRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        int inc = request.getIncrement() != null ? request.getIncrement() : 1;

        ExperientialStat stat = experientialStatRepository.findByUserIdAndModuleIdAndStatType(userId, moduleId, request.getStatType())
                .orElseGet(() -> {
                    ExperientialStat s = ExperientialStat.builder()
                            .user(user)
                            .moduleId(moduleId)
                            .statType(request.getStatType())
                            .count(0)
                            .updatedAt(Instant.now())
                            .build();
                    return experientialStatRepository.save(s);
                });

        stat.setCount(stat.getCount() + inc);
        stat = experientialStatRepository.save(stat);

        log.info("ProgressService: Experiential stat updated userId={}, moduleId={}, statType={}, count={}",
                userId, moduleId, request.getStatType(), stat.getCount());

        return ExperientialStatResponse.builder()
                .statType(stat.getStatType())
                .count(stat.getCount())
                .build();
    }
}
