package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.ExpenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, UUID> {

    List<ExpenseRecord> findByBusinessIdAndTaxYearIdOrderByTransactionDateDesc(
            UUID businessId, Integer taxYearId);

    List<ExpenseRecord> findByBusinessIdAndTaxYearIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID businessId, Integer taxYearId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(er.amount), 0) FROM ExpenseRecord er " +
           "WHERE er.business.id = :businessId AND er.taxYear.id = :taxYearId")
    BigDecimal sumByBusinessIdAndTaxYearId(
            @Param("businessId") UUID businessId,
            @Param("taxYearId") Integer taxYearId);

    @Query("SELECT er.expenseCategory, COALESCE(SUM(er.amount), 0) FROM ExpenseRecord er " +
           "WHERE er.business.id = :businessId AND er.taxYear.id = :taxYearId " +
           "GROUP BY er.expenseCategory")
    List<Object[]> sumByBusinessIdAndTaxYearIdGroupByCategory(
            @Param("businessId") UUID businessId,
            @Param("taxYearId") Integer taxYearId);

    long countByBusinessIdAndTaxYearId(UUID businessId, Integer taxYearId);
}
