package com.assesment.batchJobs.dto;

import com.assesment.batchJobs.entity.enums.JobStatus;
import com.assesment.batchJobs.entity.enums.ScheduleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class JobResponse {
    private UUID id;
    private String name;
    private ScheduleType scheduleType;
    private String cronExpression;
    private Long fixedRateMs;
    private Map<String, Object> payload;
    private Map<String, Object> retryPolicy;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
}
