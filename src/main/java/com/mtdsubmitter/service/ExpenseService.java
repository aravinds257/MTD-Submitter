package com.mtdsubmitter.service;

import com.mtdsubmitter.model.Business;
import com.mtdsubmitter.model.ExpenseRecord;
import com.mtdsubmitter.model.TaxYear;
import com.mtdsubmitter.repository.BusinessRepository;
import com.mtdsubmitter.repository.ExpenseRecordRepository;
import com.mtdsubmitter.repository.TaxYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing expense records.
 */
@Service
public class ExpenseService {

    private final ExpenseRecordRepository expenseRecordRepository;
    private final BusinessRepository businessRepository;
    private final TaxYearRepository taxYearRepository;
    private final AuditService auditService;

    public ExpenseService(ExpenseRecordRepository expenseRecordRepository,
                          BusinessRepository businessRepository,
                          TaxYearRepository taxYearRepository,
                          AuditService auditService) {
        this.expenseRecordRepository = expenseRecordRepository;
        this.businessRepository = businessRepository;
        this.taxYearRepository = taxYearRepository;
        this.auditService = auditService;
    }

    public List<ExpenseRecord> getRecords(UUID businessId, Integer taxYearId) {
        return expenseRecordRepository
                .findByBusinessIdAndTaxYearIdOrderByTransactionDateDesc(businessId, taxYearId);
    }

    public BigDecimal getTotal(UUID businessId, Integer taxYearId) {
        return expenseRecordRepository.sumByBusinessIdAndTaxYearId(businessId, taxYearId);
    }

    /**
     * Get expense totals grouped by HMRC category.
     */
    public Map<String, BigDecimal> getTotalsByCategory(UUID businessId, Integer taxYearId) {
        return expenseRecordRepository
                .sumByBusinessIdAndTaxYearIdGroupByCategory(businessId, taxYearId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    @Transactional
    public ExpenseRecord addRecord(UUID userId, UUID businessId, Integer taxYearId,
                                    LocalDate transactionDate, BigDecimal amount,
                                    String expenseCategory, String description) {
        Business business = businessRepository.getReferenceById(businessId);
        TaxYear taxYear = taxYearRepository.getReferenceById(taxYearId);

        ExpenseRecord record = ExpenseRecord.builder()
                .business(business)
                .taxYear(taxYear)
                .transactionDate(transactionDate)
                .amount(amount)
                .expenseCategory(expenseCategory)
                .description(description)
                .build();

        record = expenseRecordRepository.save(record);
        auditService.log(userId, "ExpenseRecord", record.getId(), "CREATE", null,
                String.format("{\"amount\":%.2f,\"category\":\"%s\"}", amount, expenseCategory));
        return record;
    }

    @Transactional
    public void deleteRecord(UUID recordId, UUID userId) {
        expenseRecordRepository.deleteById(recordId);
        auditService.log(userId, "ExpenseRecord", recordId, "DELETE", null, null);
    }

    public long getRecordCount(UUID businessId, Integer taxYearId) {
        return expenseRecordRepository.countByBusinessIdAndTaxYearId(businessId, taxYearId);
    }
}
