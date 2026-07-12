package com.mtdsubmitter.service;

import com.mtdsubmitter.model.IncomeRecord;
import com.mtdsubmitter.model.TaxYear;
import com.mtdsubmitter.repository.IncomeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing income records.
 */
@Service
public class IncomeService {

    private final IncomeRecordRepository incomeRecordRepository;
    private final AuditService auditService;

    public IncomeService(IncomeRecordRepository incomeRecordRepository,
                         AuditService auditService) {
        this.incomeRecordRepository = incomeRecordRepository;
        this.auditService = auditService;
    }

    public List<IncomeRecord> getRecords(UUID businessId, Integer taxYearId) {
        return incomeRecordRepository
                .findByBusinessIdAndTaxYearIdOrderByTransactionDateDesc(businessId, taxYearId);
    }

    public BigDecimal getTotal(UUID businessId, Integer taxYearId) {
        return incomeRecordRepository.sumByBusinessIdAndTaxYearId(businessId, taxYearId);
    }

    /**
     * Get income totals grouped by category.
     */
    public Map<String, BigDecimal> getTotalsByCategory(UUID businessId, Integer taxYearId) {
        return incomeRecordRepository
                .sumByBusinessIdAndTaxYearIdGroupByCategory(businessId, taxYearId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    @Transactional
    public IncomeRecord addRecord(UUID userId, UUID businessId, Integer taxYearId,
                                   LocalDate transactionDate, BigDecimal amount,
                                   String incomeCategory, String description) {
        IncomeRecord record = IncomeRecord.builder()
                .businessId(businessId)
                .taxYearId(taxYearId)
                .transactionDate(transactionDate)
                .amount(amount)
                .incomeCategory(incomeCategory)
                .description(description)
                .build();

        record = incomeRecordRepository.save(record);
        auditService.log(userId, "IncomeRecord", record.getId(), "CREATE", null,
                String.format("{\"amount\":%.2f,\"category\":\"%s\"}", amount, incomeCategory));
        return record;
    }

    @Transactional
    public void deleteRecord(UUID recordId, UUID userId) {
        incomeRecordRepository.deleteById(recordId);
        auditService.log(userId, "IncomeRecord", recordId, "DELETE", null, null);
    }

    public long getRecordCount(UUID businessId, Integer taxYearId) {
        return incomeRecordRepository.countByBusinessIdAndTaxYearId(businessId, taxYearId);
    }
}
