package com.mtdsubmitter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mtdsubmitter.model.*;
import com.mtdsubmitter.model.enums.BusinessType;
import com.mtdsubmitter.model.enums.ObligationStatus;
import com.mtdsubmitter.model.enums.SubmissionStatus;
import com.mtdsubmitter.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to aggregate financial transactions for a quarter, build HMRC-compliant payloads,
 * submit them to HMRC, and save the status in the database.
 */
@Service
public class SubmissionService {

    private final QuarterlyPeriodRepository quarterlyPeriodRepository;
    private final QuarterlySubmissionRepository quarterlySubmissionRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final HmrcApiService hmrcApiService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionService(QuarterlyPeriodRepository quarterlyPeriodRepository,
                             QuarterlySubmissionRepository quarterlySubmissionRepository,
                             IncomeRecordRepository incomeRecordRepository,
                             ExpenseRecordRepository expenseRecordRepository,
                             HmrcApiService hmrcApiService,
                             AuditService auditService) {
        this.quarterlyPeriodRepository = quarterlyPeriodRepository;
        this.quarterlySubmissionRepository = quarterlySubmissionRepository;
        this.incomeRecordRepository = incomeRecordRepository;
        this.expenseRecordRepository = expenseRecordRepository;
        this.hmrcApiService = hmrcApiService;
        this.auditService = auditService;
    }

    /**
     * Compile quarterly totals (categorized income & expenses) for a period.
     */
    public Map<String, Object> compileQuarterlyTotals(UUID periodId, UUID userId) {
        QuarterlyPeriod period = quarterlyPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Period not found"));

        if (!period.getBusiness().getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to business period");
        }

        Business business = period.getBusiness();
        Map<String, Object> totals = new HashMap<>();
        totals.put("period", period);
        totals.put("business", business);

        // Fetch records in date range
        List<IncomeRecord> incomeRecords = incomeRecordRepository
                .findByBusinessIdAndTaxYearIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        business.getId(), period.getTaxYear().getId(), period.getPeriodStart(), period.getPeriodEnd());

        List<ExpenseRecord> expenseRecords = expenseRecordRepository
                .findByBusinessIdAndTaxYearIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        business.getId(), period.getTaxYear().getId(), period.getPeriodStart(), period.getPeriodEnd());

        totals.put("incomeRecords", incomeRecords);
        totals.put("expenseRecords", expenseRecords);

        // Aggregate income by category
        BigDecimal totalIncome = BigDecimal.ZERO;
        Map<String, BigDecimal> incomeByCategory = new HashMap<>();
        for (IncomeRecord ir : incomeRecords) {
            totalIncome = totalIncome.add(ir.getAmount());
            incomeByCategory.put(ir.getIncomeCategory(),
                    incomeByCategory.getOrDefault(ir.getIncomeCategory(), BigDecimal.ZERO).add(ir.getAmount()));
        }
        totals.put("totalIncome", totalIncome);
        totals.put("incomeByCategory", incomeByCategory);

        // Aggregate expenses by category
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<String, BigDecimal> expensesByCategory = new HashMap<>();
        for (ExpenseRecord er : expenseRecords) {
            totalExpenses = totalExpenses.add(er.getAmount());
            expensesByCategory.put(er.getExpenseCategory(),
                    expensesByCategory.getOrDefault(er.getExpenseCategory(), BigDecimal.ZERO).add(er.getAmount()));
        }
        totals.put("totalExpenses", totalExpenses);
        totals.put("expensesByCategory", expensesByCategory);

        return totals;
    }

    /**
     * Submit compiled quarterly totals to HMRC and update database state.
     */
    @Transactional
    public void submitQuarter(UUID periodId, UUID userId) {
        QuarterlyPeriod period = quarterlyPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Period not found"));

        User user = period.getBusiness().getUser();
        if (!user.getId().equals(userId)) {
            throw new SecurityException("Unauthorized");
        }

        if (user.getNinoEncrypted() == null || user.getNinoEncrypted().isEmpty()) {
            throw new IllegalStateException("Your National Insurance Number (NINO) must be set in settings before submitting.");
        }

        Business business = period.getBusiness();
        if (business.getHmrcBusinessId() == null || business.getHmrcBusinessId().isEmpty()) {
            throw new IllegalStateException("This business has not been linked to an HMRC business ID yet.");
        }

        // 1. Compile totals
        Map<String, Object> compiled = compileQuarterlyTotals(periodId, userId);
        BigDecimal totalIncome = (BigDecimal) compiled.get("totalIncome");
        BigDecimal totalExpenses = (BigDecimal) compiled.get("totalExpenses");
        Map<String, BigDecimal> incomeByCategory = (Map<String, BigDecimal>) compiled.get("incomeByCategory");
        Map<String, BigDecimal> expensesByCategory = (Map<String, BigDecimal>) compiled.get("expensesByCategory");

        // 2. Build HMRC JSON Payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("periodStart", period.getPeriodStart().toString());
        payload.put("periodEnd", period.getPeriodEnd().toString());

        // Build income segment
        ObjectNode incomeNode = payload.putObject("income");
        if (business.getBusinessType() == BusinessType.SELF_EMPLOYMENT) {
            incomeNode.put("turnover", incomeByCategory.getOrDefault("turnover", BigDecimal.ZERO));
            incomeNode.put("other", incomeByCategory.getOrDefault("otherIncome", BigDecimal.ZERO));
        } else {
            // Property income
            incomeNode.put("totalRentsReceived", incomeByCategory.getOrDefault("totalRentsReceived", BigDecimal.ZERO));
            incomeNode.put("premiumsOfLeaseGrant", incomeByCategory.getOrDefault("premiumsOfLeaseGrant", BigDecimal.ZERO));
            incomeNode.put("otherPropertyIncome", incomeByCategory.getOrDefault("otherPropertyIncome", BigDecimal.ZERO));
        }

        // Build expenses segment
        ObjectNode expensesNode = payload.putObject("expenses");
        if (business.getBusinessType() == BusinessType.SELF_EMPLOYMENT) {
            // If turnover < £90,000, we can use consolidatedExpenses to simplify
            if (totalIncome.compareTo(new BigDecimal("90000")) < 0) {
                expensesNode.put("consolidatedExpenses", totalExpenses);
            } else {
                expensesByCategory.forEach(expensesNode::put);
            }
        } else {
            // Property expenses
            if (totalIncome.compareTo(new BigDecimal("90000")) < 0) {
                expensesNode.put("consolidatedExpenses", totalExpenses);
            } else {
                expensesByCategory.forEach(expensesNode::put);
            }
        }

        // 3. Create draft submission entry
        QuarterlySubmission submission = QuarterlySubmission.builder()
                .quarterlyPeriod(period)
                .submissionJson(payload.toString())
                .status(SubmissionStatus.DRAFT)
                .build();
        submission = quarterlySubmissionRepository.save(submission);

        // 4. Send to HMRC API
        JsonNode responseNode;
        try {
            if (business.getBusinessType() == BusinessType.SELF_EMPLOYMENT) {
                responseNode = hmrcApiService.submitSelfEmploymentQuarter(
                        userId, user.getNinoEncrypted(), business.getHmrcBusinessId(),
                        period.getTaxYear().getLabel(), payload).block();
            } else {
                responseNode = hmrcApiService.submitPropertyQuarter(
                        userId, user.getNinoEncrypted(), business.getHmrcBusinessId(),
                        period.getTaxYear().getLabel(), payload).block();
            }
        } catch (Exception e) {
            submission.setStatus(SubmissionStatus.REJECTED);
            quarterlySubmissionRepository.save(submission);
            throw new RuntimeException("HMRC API Submission Failed: " + e.getMessage(), e);
        }

        // 5. Update submission and period status on success
        String submissionId = responseNode != null && responseNode.has("submissionId")
                ? responseNode.get("submissionId").asText() : UUID.randomUUID().toString();

        submission.setStatus(SubmissionStatus.ACCEPTED);
        submission.setResponseJson(responseNode != null ? responseNode.toString() : "{}");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setHmrcSubmissionId(submissionId);
        quarterlySubmissionRepository.save(submission);

        period.setStatus(ObligationStatus.FULFILLED);
        period.setSubmittedAt(LocalDateTime.now());
        period.setHmrcSubmissionId(submissionId);
        quarterlyPeriodRepository.save(period);

        auditService.log(userId, "QuarterlySubmission", submission.getId(), "SUBMIT_QUARTER",
                null, String.format("{\"submissionId\":\"%s\",\"income\":%.2f}", submissionId, totalIncome));
    }
}
