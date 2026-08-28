package com.healthcare.user.controller;

import com.healthcare.user.dto.LoginRequest;
import com.healthcare.user.dto.LoginResponse;
import com.healthcare.user.dto.LogoutRequest;
import com.healthcare.user.dto.RefreshTokenRequest;
import com.healthcare.user.dto.RegisterRequest;
import com.healthcare.user.dto.RegisterResponse;
import com.healthcare.user.model.AuthAuditLog;
import com.healthcare.user.model.DoctorProfile;
import com.healthcare.user.repository.AuthAuditLogRepository;
import com.healthcare.user.service.DoctorVerificationService;
import com.healthcare.user.service.KeycloakAuthService;
import com.healthcare.user.service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final TotpService totpService;
    private final DoctorVerificationService doctorVerificationService;
    private final AuthAuditLogRepository authAuditLogRepository;

    @Data
    public static class TotpSetupResponse {
        private String secret;
        private String qrCodeUri;
    }

    @Data
    public static class TotpVerifyRequest {
        private String secret;
        private String code;
    }

    /**
     * Register a new user in Keycloak IAM and PostgreSQL database.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest httpRequest) {

        registerRequest.setClientIp(httpRequest.getRemoteAddr());
        registerRequest.setUserAgent(httpRequest.getHeader("User-Agent"));

        log.info("Received user registration request for email: {}, role: {}",
                registerRequest.getEmail(), registerRequest.getRole());
        RegisterResponse response = keycloakAuthService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user with Keycloak IAM Direct Access Grants and TOTP 2FA enforcement.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest) {
        
        loginRequest.setClientIp(httpRequest.getRemoteAddr());
        loginRequest.setUserAgent(httpRequest.getHeader("User-Agent"));

        log.info("Received login request for user: {}", loginRequest.getUsernameOrEmail());
        LoginResponse response = keycloakAuthService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Keycloak Single Sign-Out: Revokes the refresh token and terminates the user's Keycloak session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @Valid @RequestBody LogoutRequest logoutRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        String userId = (jwt != null) ? jwt.getSubject() : null;
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Received logout request from user: {}", userId);
        Map<String, Object> result = keycloakAuthService.logout(logoutRequest, userId, clientIp, userAgent);
        return ResponseEntity.ok(result);
    }

    /**
     * Refresh access token via Keycloak OpenID Connect refresh_token grant.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = keycloakAuthService.refreshToken(refreshRequest, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * Get authenticated user profile and Keycloak realm roles.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
                "subject", jwt.getSubject(),
                "claims", jwt.getClaims(),
                "issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : "",
                "expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : ""
        ));
    }

    /**
     * Generate TOTP 2FA secret and QR code URI for HIPAA compliant multi-factor authentication.
     */
    @PostMapping("/totp/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setupTotp(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String secret = totpService.generateSecret();
        String qrUri = totpService.getQrDataUri(email != null ? email : "patient@healthcare.com", secret);

        TotpSetupResponse response = new TotpSetupResponse();
        response.setSecret(secret);
        response.setQrCodeUri(qrUri);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify TOTP 6-digit code.
     */
    @PostMapping("/totp/verify")
    public ResponseEntity<Map<String, Object>> verifyTotp(@RequestBody TotpVerifyRequest request) {
        boolean valid = totpService.verifyCode(request.getSecret(), request.getCode());
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "timestamp", System.currentTimeMillis(),
                "hipaaComplianceLevel", "ENFORCED_2FA"
        ));
    }

    /**
     * Retrieve HIPAA audit logs for security monitoring.
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(authAuditLogRepository.findTop50ByOrderByTimestampDesc());
    }

    /**
     * Admin verification of physician medical license.
     */
    @PostMapping("/doctor/verify-license")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorProfile> verifyDoctorLicense(
            @RequestParam("doctorId") String doctorId,
            @AuthenticationPrincipal Jwt jwt) {
        String adminId = jwt.getSubject();
        return doctorVerificationService.verifyDoctor(doctorId, adminId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
