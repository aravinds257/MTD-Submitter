package com.mtdsubmitter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to build HMRC-compliant Fraud Prevention Headers.
 * This is a mandatory audit requirement for all Making Tax Digital (MTD) software.
 */
@Service
public class FraudPreventionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build the map of Gov-Client headers required by HMRC for WEB_APP_VIA_SERVER connection method.
     */
    public Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return headers; // fallback
        }

        HttpServletRequest request = attributes.getRequest();

        // 1. Mandatory connection method
        headers.put("Gov-Client-Connection-Method", "WEB_APP_VIA_SERVER");

        // 2. Client browser data (parsed from cookie)
        Map<String, Object> fpCookieData = parseFraudPreventionCookie(request);

        // Gov-Client-Device-ID (mandatory UUID)
        String deviceId = (String) fpCookieData.getOrDefault("deviceId", "00000000-0000-0000-0000-000000000000");
        headers.put("Gov-Client-Device-ID", deviceId);

        // Gov-Client-User-Agent (mandatory)
        String browserUserAgent = request.getHeader("User-Agent");
        headers.put("Gov-Client-User-Agent", browserUserAgent != null ? browserUserAgent : "Unknown");

        // Gov-Client-Timezone (mandatory)
        String timezoneOffset = (String) fpCookieData.getOrDefault("timezoneOffset", "UTC+00:00");
        headers.put("Gov-Client-Timezone", timezoneOffset);

        // Gov-Client-Screens (mandatory)
        String screens = String.format("width=%s&height=%s&scaling-factor=%s&colour-depth=%s",
                fpCookieData.getOrDefault("screenWidth", "1920"),
                fpCookieData.getOrDefault("screenHeight", "1080"),
                fpCookieData.getOrDefault("scalingFactor", "1"),
                fpCookieData.getOrDefault("screenColourDepth", "24")
        );
        headers.put("Gov-Client-Screens", screens);

        // Gov-Client-Window-Size (mandatory)
        String windowSize = String.format("width=%s&height=%s",
                fpCookieData.getOrDefault("windowWidth", "1024"),
                fpCookieData.getOrDefault("windowHeight", "768")
        );
        headers.put("Gov-Client-Window-Size", windowSize);

        // Gov-Client-Browser-Plugins (mandatory list of plugins)
        String plugins = (String) fpCookieData.getOrDefault("plugins", "");
        headers.put("Gov-Client-Browser-Plugins", plugins);

        // Gov-Client-Browser-Do-Not-Track (mandatory)
        Boolean doNotTrack = (Boolean) fpCookieData.getOrDefault("doNotTrack", false);
        headers.put("Gov-Client-Browser-Do-Not-Track", String.valueOf(doNotTrack));

        // 3. IP tracking (mandatory)
        String clientIp = getClientIp(request);
        headers.put("Gov-Client-Public-IP", clientIp);
        
        // Host IP (our server public IP) - fallback to localhost for sandboxing
        headers.put("Gov-Vendor-Public-IP", "127.0.0.1"); 
        
        // 4. Software versioning
        headers.put("Gov-Vendor-Version", "mtd-submitter=1.0.0");
        
        // License ID (mandatory - your unique identifier as developer)
        headers.put("Gov-Vendor-License-ID", "mtd-submitter-license-001");

        return headers;
    }

    private Map<String, Object> parseFraudPreventionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("mtd_fp_data".equals(cookie.getName())) {
                    try {
                        String decodedJson = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                        return objectMapper.readValue(decodedJson, HashMap.class);
                    } catch (Exception ignored) {
                        // ignore and fall back
                    }
                }
            }
        }
        return new HashMap<>();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
