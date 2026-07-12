package com.mtdsubmitter.controller;

import com.mtdsubmitter.model.User;
import com.mtdsubmitter.service.HmrcAuthService;
import com.mtdsubmitter.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Controller to handle HMRC Government Gateway OAuth2 authorization code flow redirection.
 */
@Controller
public class HmrcOAuthController {

    private final HmrcAuthService hmrcAuthService;
    private final UserService userService;

    public HmrcOAuthController(HmrcAuthService hmrcAuthService, UserService userService) {
        this.hmrcAuthService = hmrcAuthService;
        this.userService = userService;
    }

    /**
     * Start connection flow: redirect user to HMRC Government Gateway.
     */
    @GetMapping("/hmrc/connect")
    public String connect(HttpSession session) {
        // Generate random state to prevent CSRF
        String state = UUID.randomUUID().toString();
        session.setAttribute("hmrc_oauth_state", state);

        String authUrl = hmrcAuthService.getAuthorizationUrl(state);
        return "redirect:" + authUrl;
    }

    /**
     * OAuth Callback endpoint registered with HMRC Developer Hub.
     */
    @GetMapping("/callback/hmrc")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           @RequestParam(required = false, name = "error_description") String errorDescription,
                           HttpSession session,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {

        // 1. Check for error from HMRC
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", "Failed to connect to HMRC: " + errorDescription);
            return "redirect:/dashboard";
        }

        // 2. Validate state to protect against CSRF
        String savedState = (String) session.getAttribute("hmrc_oauth_state");
        session.removeAttribute("hmrc_oauth_state");

        if (savedState == null || !savedState.equals(state)) {
            redirectAttributes.addFlashAttribute("error", "OAuth session verification failed. Please try again.");
            return "redirect:/dashboard";
        }

        if (code == null || code.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No authorization code returned from HMRC.");
            return "redirect:/dashboard";
        }

        // 3. Exchange code for token and save
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            hmrcTokenRepositoryExchange(user.getId(), code);
            redirectAttributes.addFlashAttribute("success", "Successfully connected to HMRC Government Gateway!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error exchanging OAuth code: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    private void hmrcTokenRepositoryExchange(UUID userId, String code) {
        hmrcAuthService.exchangeCode(userId, code).block();
    }
}
