package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.IncomeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, UUID> {

    List<IncomeRecord> findByBusinessIdAndTaxYearIdOrderByTransactionDateDesc(
            UUID businessId, Integer taxYearId);

    List<IncomeRecord> findByBusinessIdAndTaxYearIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID businessId, Integer taxYearId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(ir.amount), 0) FROM IncomeRecord ir " +
           "WHERE ir.business.id = :businessId AND ir.taxYear.id = :taxYearId")
    BigDecimal sumByBusinessIdAndTaxYearId(
            @Param("businessId") UUID businessId,
            @Param("taxYearId") Integer taxYearId);

    @Query("SELECT ir.incomeCategory, COALESCE(SUM(ir.amount), 0) FROM IncomeRecord ir " +
           "WHERE ir.business.id = :businessId AND ir.taxYear.id = :taxYearId " +
           "GROUP BY ir.incomeCategory")
    List<Object[]> sumByBusinessIdAndTaxYearIdGroupByCategory(
            @Param("businessId") UUID businessId,
            @Param("taxYearId") Integer taxYearId);

    long countByBusinessIdAndTaxYearId(UUID businessId, Integer taxYearId);
}
