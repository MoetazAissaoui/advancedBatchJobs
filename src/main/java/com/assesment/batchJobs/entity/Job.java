package com.assesment.batchJobs.entity;

import com.assesment.batchJobs.entity.enums.JobStatus;
import com.assesment.batchJobs.entity.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    private String cronExpression;
    private Long fixedRateMs;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> retryPolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;

    @Version
    private Long version;
}
