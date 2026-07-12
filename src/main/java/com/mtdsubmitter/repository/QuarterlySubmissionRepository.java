package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.QuarterlySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuarterlySubmissionRepository extends JpaRepository<QuarterlySubmission, UUID> {
    List<QuarterlySubmission> findByQuarterlyPeriodIdOrderByCreatedAtDesc(UUID quarterlyPeriodId);
}
