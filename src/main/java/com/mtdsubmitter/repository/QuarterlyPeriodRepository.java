package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.QuarterlyPeriod;
import com.mtdsubmitter.model.enums.ObligationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuarterlyPeriodRepository extends JpaRepository<QuarterlyPeriod, UUID> {

    List<QuarterlyPeriod> findByBusinessIdAndTaxYearIdOrderByQuarterNumberAsc(
            UUID businessId, Integer taxYearId);

    Optional<QuarterlyPeriod> findByBusinessIdAndTaxYearIdAndQuarterNumber(
            UUID businessId, Integer taxYearId, Integer quarterNumber);

    @Query("SELECT qp FROM QuarterlyPeriod qp WHERE qp.status = :status " +
           "AND qp.dueDate <= :date ORDER BY qp.dueDate ASC")
    List<QuarterlyPeriod> findOverdue(
            @Param("status") ObligationStatus status,
            @Param("date") LocalDate date);

    @Query("SELECT qp FROM QuarterlyPeriod qp WHERE qp.business.user.id = :userId " +
           "AND qp.status = 'OPEN' ORDER BY qp.dueDate ASC")
    List<QuarterlyPeriod> findNextDueByUserId(@Param("userId") UUID userId);
}
