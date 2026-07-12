package com.mtdsubmitter.controller;

import com.mtdsubmitter.model.Business;
import com.mtdsubmitter.model.TaxYear;
import com.mtdsubmitter.model.User;
import com.mtdsubmitter.model.enums.BusinessType;
import com.mtdsubmitter.model.enums.PropertyExpenseCategory;
import com.mtdsubmitter.model.enums.SelfEmploymentExpenseCategory;
import com.mtdsubmitter.service.BusinessService;
import com.mtdsubmitter.service.ExpenseService;
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
 * Controller for managing expense records.
 */
@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final BusinessService businessService;
    private final UserService userService;

    public ExpenseController(ExpenseService expenseService,
                             BusinessService businessService,
                             UserService userService) {
        this.expenseService = expenseService;
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
            Business business = businessService.getBusiness(businessId, user.getId())
                    .orElseThrow(() -> new RuntimeException("Business not found"));

            model.addAttribute("selectedBusinessId", businessId);
            model.addAttribute("selectedBusiness", business);
            model.addAttribute("records", expenseService.getRecords(businessId, taxYear.getId()));
            model.addAttribute("total", expenseService.getTotal(businessId, taxYear.getId()));
            model.addAttribute("totalsByCategory", expenseService.getTotalsByCategory(businessId, taxYear.getId()));
            model.addAttribute("taxYear", taxYear);
        }

        return "expense/list";
    }

    @GetMapping("/add")
    public String addForm(@RequestParam(required = false) UUID businessId,
                          Authentication auth, Model model) {
        User user = getUser(auth);
        ExpenseForm form = new ExpenseForm();
        if (businessId != null) {
            form.setBusinessId(businessId);
        }
        model.addAttribute("expenseForm", form);
        model.addAttribute("businesses", businessService.getActiveBusinesses(user.getId()));
        model.addAttribute("selfEmploymentCategories", SelfEmploymentExpenseCategory.values());
        model.addAttribute("propertyCategories", PropertyExpenseCategory.values());
        return "expense/add";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("expenseForm") ExpenseForm form,
                      BindingResult result, Authentication auth, Model model,
                      RedirectAttributes redirectAttributes) {
        User user = getUser(auth);

        if (result.hasErrors()) {
            model.addAttribute("businesses", businessService.getActiveBusinesses(user.getId()));
            model.addAttribute("selfEmploymentCategories", SelfEmploymentExpenseCategory.values());
            model.addAttribute("propertyCategories", PropertyExpenseCategory.values());
            return "expense/add";
        }

        TaxYear taxYear = businessService.getCurrentTaxYear();
        expenseService.addRecord(user.getId(), form.getBusinessId(), taxYear.getId(),
                form.getTransactionDate(), form.getAmount(),
                form.getExpenseCategory(), form.getDescription());

        redirectAttributes.addFlashAttribute("success", "Expense recorded: £" + form.getAmount());
        return "redirect:/expenses?businessId=" + form.getBusinessId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @RequestParam UUID businessId,
                         Authentication auth, RedirectAttributes redirectAttributes) {
        User user = getUser(auth);
        expenseService.deleteRecord(id, user.getId());
        redirectAttributes.addFlashAttribute("success", "Expense record deleted.");
        return "redirect:/expenses?businessId=" + businessId;
    }

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Data
    public static class ExpenseForm {
        @NotNull(message = "Please select a business")
        private UUID businessId;

        @NotNull(message = "Date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate transactionDate;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Category is required")
        private String expenseCategory;

        private String description;
    }
}
