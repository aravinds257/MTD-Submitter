package com.mtdsubmitter.controller;

import com.mtdsubmitter.model.Business;
import com.mtdsubmitter.model.User;
import com.mtdsubmitter.model.enums.AccountingType;
import com.mtdsubmitter.model.enums.BusinessType;
import com.mtdsubmitter.service.BusinessService;
import com.mtdsubmitter.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Controller for managing businesses (self-employment + property income sources).
 */
@Controller
@RequestMapping("/businesses")
public class BusinessController {

    private final BusinessService businessService;
    private final UserService userService;

    public BusinessController(BusinessService businessService, UserService userService) {
        this.businessService = businessService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        User user = getUser(auth);
        model.addAttribute("businesses", businessService.getActiveBusinesses(user.getId()));
        return "business/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("businessForm", new BusinessForm());
        model.addAttribute("businessTypes", BusinessType.values());
        model.addAttribute("accountingTypes", AccountingType.values());
        return "business/add";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("businessForm") BusinessForm form,
                      BindingResult result, Authentication auth, Model model,
                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("businessTypes", BusinessType.values());
            model.addAttribute("accountingTypes", AccountingType.values());
            return "business/add";
        }

        User user = getUser(auth);
        businessService.createBusiness(user.getId(), form.getTradingName(),
                form.getBusinessType(), form.getAccountingType(), form.getDescription());

        redirectAttributes.addFlashAttribute("success", "Business added successfully!");
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable UUID id, Authentication auth, Model model) {
        User user = getUser(auth);
        Business business = businessService.getBusiness(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Business not found"));
        model.addAttribute("business", business);
        return "business/view";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable UUID id, Authentication auth,
                             RedirectAttributes redirectAttributes) {
        User user = getUser(auth);
        businessService.deactivateBusiness(id, user.getId());
        redirectAttributes.addFlashAttribute("success", "Business removed.");
        return "redirect:/businesses";
    }

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Data
    public static class BusinessForm {
        @NotBlank(message = "Business name is required")
        private String tradingName;

        @NotNull(message = "Business type is required")
        private BusinessType businessType;

        private String accountingType = "CASH";
        private String description;
    }
}
