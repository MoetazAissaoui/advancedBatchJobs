package com.assesment.batchJobs.service;

import com.assesment.batchJobs.dto.*;
import com.assesment.batchJobs.entity.Job;
import com.assesment.batchJobs.entity.enums.JobStatus;
import com.assesment.batchJobs.entity.enums.ScheduleType;
import com.assesment.batchJobs.repository.JobRepository;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final Map<UUID, List<ExecutionRecord>> executionHistory = new HashMap<>();

    @Transactional
    public JobResponse createJob(JobRequest request) {
        if (jobRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Job with this name already exists");
        }

        Job job = new Job();
        job.setName(request.getName());
        job.setScheduleType(request.getScheduleType());
        job.setCronExpression(request.getCronExpression());
        job.setFixedRateMs(request.getFixedRateMs());
        job.setPayload(request.getPayload());
        job.setRetryPolicy(request.getRetryPolicy());
        job.setStatus(JobStatus.ACTIVE);

        calculateNextRun(job);
        Job savedJob = jobRepository.save(job);
        log.debug("Job {} created, will be executed on {}",job.getName(), job.getNextRunAt());
        return mapToResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id) {
        return jobRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    @Transactional
    public JobResponse updateJob(UUID id, JobUpdateRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getName().equals(request.getName()) &&
                jobRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Job with this name already exists");
        }

        job.setName(request.getName());
        job.setScheduleType(request.getScheduleType());
        job.setCronExpression(request.getCronExpression());
        job.setFixedRateMs(request.getFixedRateMs());
        job.setPayload(request.getPayload());
        job.setRetryPolicy(request.getRetryPolicy());

        calculateNextRun(job);
        Job updatedJob = jobRepository.save(job);

        return mapToResponse(updatedJob);
    }

    @Transactional
    public JobResponse updateStatus(UUID id, JobStatus status) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() == JobStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled jobs cannot be modified");
        }

        job.setStatus(status);
        Job updatedJob = jobRepository.save(job);

        return mapToResponse(updatedJob);
    }

    @Scheduled(fixedRate = 5)
    @Transactional
    public void executeDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> dueJobs = jobRepository.findByNextRunAtLessThanEqualAndStatus(now, JobStatus.ACTIVE);

        dueJobs.forEach(job -> {
            try {
                executeJob(job);
                resetRetryPolicy(job);
                logExecution(job.getId(), true, "Executed successfully");
            } catch (Exception e) {
                log.error("Error executing job {}", job.getId(), e);
                handleRetryPolicy(job);
                logExecution(job.getId(), false, e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public ExecutionHistoryResponse getExecutionHistory(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found");
        }

        List<ExecutionRecord> records = executionHistory.getOrDefault(id, Collections.emptyList());

        return ExecutionHistoryResponse.builder()
                .jobId(id)
                .totalExecutions(records.size())
                .successfulExecutions(records.stream().filter(ExecutionRecord::isSuccess).count())
                .failedExecutions(records.stream().filter(r -> !r.isSuccess()).count())
                .executions(records)
                .build();
    }

    private void executeJob(Job job) {
        log.info("Executing job: {}", job.getName());
        log.debug("Job Payload content is {}", job.getPayload());
        // Simulate job execution
        if (job.getPayload() != null && job.getPayload().containsKey("simulateFailure")) {
            throw new RuntimeException("Simulated job failure");
        }

        job.setLastRunAt(LocalDateTime.now());
        calculateNextRun(job);
        log.info("Next Job forecast execution will occur in {}.", job.getNextRunAt());
        jobRepository.save(job);
    }

    private void calculateNextRun(Job job) {
        if (job.getScheduleType() == ScheduleType.CRON) {
            try {
                // Define the cron definition for Quartz format
                CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                CronParser parser = new CronParser(cronDefinition);

                // Parse the expression
                Cron quartzCron = parser.parse(job.getCronExpression());

                // Calculate next execution
                ExecutionTime executionTime = ExecutionTime.forCron(quartzCron);
                Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(ZonedDateTime.now());

                if (nextExecution.isPresent()) {
                    job.setNextRunAt(nextExecution.get().toLocalDateTime());
                } else {
                    throw new IllegalArgumentException("Could not calculate next execution time");
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid cron expression for job {}: {}", job.getId(), job.getCronExpression(), e);
                job.setStatus(JobStatus.CANCELLED);
                throw new IllegalArgumentException("Invalid cron expression: " + job.getCronExpression());
            }
        } else {
            job.setNextRunAt(LocalDateTime.now().plusNanos(job.getFixedRateMs() * 1_000_000));
        }
    }
    private void handleRetryPolicy(Job job) {
        Map<String, Object> retryPolicy = job.getRetryPolicy() != null ?
                job.getRetryPolicy() : new HashMap<>();

        int maxAttempts = (int) retryPolicy.getOrDefault("maxAttempts", 3);
        int currentAttempt = (int) retryPolicy.getOrDefault("currentAttempt", 0);
        long backoffMs = (long) retryPolicy.getOrDefault("backoffMs", 5000);

        if (currentAttempt < maxAttempts) {
            retryPolicy.put("currentAttempt", currentAttempt + 1);
            job.setRetryPolicy(retryPolicy);
            job.setNextRunAt(LocalDateTime.now().plusNanos(backoffMs * 1_000_000));
            jobRepository.save(job);
        } else {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.save(job);
        }
    }

    private void resetRetryPolicy(Job job) {
        if (job.getRetryPolicy() != null && job.getRetryPolicy().containsKey("currentAttempt")) {
            job.getRetryPolicy().put("currentAttempt", 0);
            jobRepository.save(job);
        }
    }

    private void logExecution(UUID jobId, boolean success, String message) {
        ExecutionRecord record = ExecutionRecord.builder()
                .timestamp(LocalDateTime.now())
                .success(success)
                .message(message)
                .build();

        executionHistory.computeIfAbsent(jobId, k -> new ArrayList<>()).add(record);
    }

    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .scheduleType(job.getScheduleType())
                .cronExpression(job.getCronExpression())
                .fixedRateMs(job.getFixedRateMs())
                .payload(job.getPayload())
                .retryPolicy(job.getRetryPolicy())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .lastRunAt(job.getLastRunAt())
                .nextRunAt(job.getNextRunAt())
                .build();
    }
}