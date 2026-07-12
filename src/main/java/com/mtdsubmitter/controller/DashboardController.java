package com.mtdsubmitter.controller;

import com.mtdsubmitter.model.Business;
import com.mtdsubmitter.model.QuarterlyPeriod;
import com.mtdsubmitter.model.TaxYear;
import com.mtdsubmitter.model.User;
import com.mtdsubmitter.repository.*;
import com.mtdsubmitter.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Main dashboard controller — the heart of the application.
 * Shows obligation status, deadlines, and financial summaries.
 */
@Controller
public class DashboardController {

    private final UserService userService;
    private final BusinessRepository businessRepository;
    private final TaxYearRepository taxYearRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final QuarterlyPeriodRepository quarterlyPeriodRepository;
    private final HmrcTokenRepository hmrcTokenRepository;

    public DashboardController(UserService userService,
                               BusinessRepository businessRepository,
                               TaxYearRepository taxYearRepository,
                               IncomeRecordRepository incomeRecordRepository,
                               ExpenseRecordRepository expenseRecordRepository,
                               QuarterlyPeriodRepository quarterlyPeriodRepository,
                               HmrcTokenRepository hmrcTokenRepository) {
        this.userService = userService;
        this.businessRepository = businessRepository;
        this.taxYearRepository = taxYearRepository;
        this.incomeRecordRepository = incomeRecordRepository;
        this.expenseRecordRepository = expenseRecordRepository;
        this.quarterlyPeriodRepository = quarterlyPeriodRepository;
        this.hmrcTokenRepository = hmrcTokenRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check HMRC connection status
        boolean hmrcConnected = hmrcTokenRepository.findByUserId(user.getId()).isPresent();
        model.addAttribute("hmrcConnected", hmrcConnected);

        // Get current tax year
        Optional<TaxYear> currentTaxYear = taxYearRepository.findCurrent();
        model.addAttribute("currentTaxYear", currentTaxYear.orElse(null));

        // Get user's active businesses
        List<Business> businesses = businessRepository.findByUserIdAndIsActiveTrue(user.getId());
        model.addAttribute("businesses", businesses);

        // Build business summaries
        if (currentTaxYear.isPresent()) {
            TaxYear taxYear = currentTaxYear.get();
            List<Map<String, Object>> businessSummaries = new ArrayList<>();

            LocalDate nextDeadline = null;

            for (Business business : businesses) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("business", business);

                // Income & expense totals
                BigDecimal totalIncome = incomeRecordRepository
                        .sumByBusinessIdAndTaxYearId(business.getId(), taxYear.getId());
                BigDecimal totalExpenses = expenseRecordRepository
                        .sumByBusinessIdAndTaxYearId(business.getId(), taxYear.getId());
                BigDecimal estimatedProfit = totalIncome.subtract(totalExpenses);

                summary.put("totalIncome", totalIncome);
                summary.put("totalExpenses", totalExpenses);
                summary.put("estimatedProfit", estimatedProfit);

                // Quarterly obligations
                List<QuarterlyPeriod> quarters = quarterlyPeriodRepository
                        .findByBusinessIdAndTaxYearIdOrderByQuarterNumberAsc(
                                business.getId(), taxYear.getId());
                summary.put("quarters", quarters);

                // Find next deadline
                for (QuarterlyPeriod qp : quarters) {
                    if ("OPEN".equals(qp.getStatus().name())
                            && (nextDeadline == null || qp.getDueDate().isBefore(nextDeadline))) {
                        nextDeadline = qp.getDueDate();
                    }
                }

                businessSummaries.add(summary);
            }

            model.addAttribute("businessSummaries", businessSummaries);

            // Days until next deadline
            if (nextDeadline != null) {
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextDeadline);
                model.addAttribute("nextDeadline", nextDeadline);
                model.addAttribute("daysUntilDeadline", daysUntil);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("hasActiveAccess", userService.hasActiveAccess(user));

        return "dashboard";
    }
}
