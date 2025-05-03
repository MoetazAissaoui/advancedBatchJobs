package com.assesment.batchJobs.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExecutionHistoryResponse {
    private UUID jobId;
    private long totalExecutions;
    private long successfulExecutions;
    private long failedExecutions;
    private List<ExecutionRecord> executions;
}