package com.assesment.batchJobs.dto;

import com.assesment.batchJobs.entity.enums.ScheduleType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class JobRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private ScheduleType scheduleType;

    @Pattern(regexp = "^$|^(\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+)$",
            message = "Invalid cron expression")
    private String cronExpression;

    @Min(1000)
    private Long fixedRateMs;

    private Map<String, Object> payload;
    private Map<String, Object> retryPolicy;
}
