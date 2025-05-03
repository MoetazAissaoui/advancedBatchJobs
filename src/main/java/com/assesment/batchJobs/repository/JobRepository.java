package com.assesment.batchJobs.repository;

import com.assesment.batchJobs.entity.Job;
import com.assesment.batchJobs.entity.enums.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    Optional<Job> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.nextRunAt <= :now AND j.status = :status")
    List<Job> findDueJobs(LocalDateTime now, JobStatus status);
    List<Job> findByNextRunAtLessThanEqualAndStatus(LocalDateTime nextRunAt, JobStatus status);
}