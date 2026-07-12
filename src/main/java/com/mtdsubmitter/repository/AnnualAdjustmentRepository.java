package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.AnnualAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnualAdjustmentRepository extends JpaRepository<AnnualAdjustment, UUID> {
    List<AnnualAdjustment> findByBusinessIdAndTaxYearId(UUID businessId, Integer taxYearId);
}
