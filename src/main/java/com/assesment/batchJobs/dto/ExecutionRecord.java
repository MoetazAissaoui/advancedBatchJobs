package com.assesment.batchJobs.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionRecord {
    private LocalDateTime timestamp;
    private boolean success;
    private String message;
}