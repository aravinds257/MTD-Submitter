package com.mtdsubmitter.controller;

import com.mtdsubmitter.model.QuarterlySubmission;
import com.mtdsubmitter.model.User;
import com.mtdsubmitter.repository.QuarterlySubmissionRepository;
import com.mtdsubmitter.service.SubmissionService;
import com.mtdsubmitter.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller to handle viewing quarterly submission history and submitting quarterly updates to HMRC.
 */
@Controller
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final UserService userService;
    private final QuarterlySubmissionRepository quarterlySubmissionRepository;

    public SubmissionController(SubmissionService submissionService,
                                UserService userService,
                                QuarterlySubmissionRepository quarterlySubmissionRepository) {
        this.submissionService = submissionService;
        this.userService = userService;
        this.quarterlySubmissionRepository = quarterlySubmissionRepository;
    }

    /**
     * Show past submissions list.
     */
    @GetMapping
    public String list(Authentication auth, Model model) {
        User user = getUser(auth);

        // Fetch submissions that belong to this user's businesses
        List<QuarterlySubmission> submissions = quarterlySubmissionRepository.findAll().stream()
                .filter(s -> s.getQuarterlyPeriod().getBusiness().getUser().getId().equals(user.getId()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .collect(Collectors.toList());

        model.addAttribute("submissions", submissions);
        return "submission/list";
    }

    /**
     * Review the financial totals for the selected quarter before submitting.
     */
    @GetMapping("/review/{periodId}")
    public String review(@PathVariable UUID periodId, Authentication auth, Model model) {
        User user = getUser(auth);

        try {
            Map<String, Object> compiledData = submissionService.compileQuarterlyTotals(periodId, user.getId());
            model.addAllAttributes(compiledData);
            return "submission/review";
        } catch (SecurityException e) {
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            return "redirect:/dashboard";
        }
    }

    /**
     * Confirm and submit the quarterly update to HMRC.
     */
    @PostMapping("/submit/{periodId}")
    public String submit(@PathVariable UUID periodId, Authentication auth,
                         RedirectAttributes redirectAttributes) {
        User user = getUser(auth);

        try {
            submissionService.submitQuarter(periodId, user.getId());
            redirectAttributes.addFlashAttribute("success", "Quarterly update submitted successfully to HMRC!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
            return "redirect:/submissions/review/" + periodId;
        }

        return "redirect:/dashboard";
    }

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
