package com.mtdsubmitter.controller;

import com.mtdsubmitter.model.Business;
import com.mtdsubmitter.model.TaxYear;
import com.mtdsubmitter.model.User;
import com.mtdsubmitter.model.enums.IncomeCategory;
import com.mtdsubmitter.service.BusinessService;
import com.mtdsubmitter.service.IncomeService;
import com.mtdsubmitter.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for managing income records.
 */
@Controller
@RequestMapping("/income")
public class IncomeController {

    private final IncomeService incomeService;
    private final BusinessService businessService;
    private final UserService userService;

    public IncomeController(IncomeService incomeService,
                            BusinessService businessService,
                            UserService userService) {
        this.incomeService = incomeService;
        this.businessService = businessService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) UUID businessId,
                       Authentication auth, Model model) {
        User user = getUser(auth);
        List<Business> businesses = businessService.getActiveBusinesses(user.getId());
        model.addAttribute("businesses", businesses);

        if (businessId != null) {
            TaxYear taxYear = businessService.getCurrentTaxYear();
            model.addAttribute("selectedBusinessId", businessId);
            model.addAttribute("records", incomeService.getRecords(businessId, taxYear.getId()));
            model.addAttribute("total", incomeService.getTotal(businessId, taxYear.getId()));
            model.addAttribute("taxYear", taxYear);
        }

        return "income/list";
    }

    @GetMapping("/add")
    public String addForm(Authentication auth, Model model) {
        User user = getUser(auth);
        model.addAttribute("incomeForm", new IncomeForm());
        model.addAttribute("businesses", businessService.getActiveBusinesses(user.getId()));
        model.addAttribute("categories", IncomeCategory.values());
        return "income/add";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("incomeForm") IncomeForm form,
                      BindingResult result, Authentication auth, Model model,
                      RedirectAttributes redirectAttributes) {
        User user = getUser(auth);

        if (result.hasErrors()) {
            model.addAttribute("businesses", businessService.getActiveBusinesses(user.getId()));
            model.addAttribute("categories", IncomeCategory.values());
            return "income/add";
        }

        TaxYear taxYear = businessService.getCurrentTaxYear();
        incomeService.addRecord(user.getId(), form.getBusinessId(), taxYear.getId(),
                form.getTransactionDate(), form.getAmount(),
                form.getIncomeCategory(), form.getDescription());

        redirectAttributes.addFlashAttribute("success", "Income recorded: £" + form.getAmount());
        return "redirect:/income?businessId=" + form.getBusinessId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @RequestParam UUID businessId,
                         Authentication auth, RedirectAttributes redirectAttributes) {
        User user = getUser(auth);
        incomeService.deleteRecord(id, user.getId());
        redirectAttributes.addFlashAttribute("success", "Income record deleted.");
        return "redirect:/income?businessId=" + businessId;
    }

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Data
    public static class IncomeForm {
        @NotNull(message = "Please select a business")
        private UUID businessId;

        @NotNull(message = "Date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate transactionDate;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Category is required")
        private String incomeCategory;

        private String description;
    }
}
