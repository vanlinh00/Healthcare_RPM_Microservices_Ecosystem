package com.healthcare.user.controller;

import com.healthcare.user.dto.LoginRequest;
import com.healthcare.user.dto.LoginResponse;
import com.healthcare.user.dto.LogoutRequest;
import com.healthcare.user.dto.RefreshTokenRequest;
import com.healthcare.user.dto.RegisterRequest;
import com.healthcare.user.dto.RegisterResponse;
import com.healthcare.user.exception.ApiErrorResponse;
import com.healthcare.user.model.AuthAuditLog;
import com.healthcare.user.model.DoctorProfile;
import com.healthcare.user.repository.AuthAuditLogRepository;
import com.healthcare.user.service.DoctorVerificationService;
import com.healthcare.user.service.KeycloakAuthService;
import com.healthcare.user.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication & MFA", description = "Keycloak 24 IAM Direct Access Grants, user registration, single sign-out, token refresh, and HIPAA RFC 6238 TOTP 2FA")
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final TotpService totpService;
    private final DoctorVerificationService doctorVerificationService;
    private final AuthAuditLogRepository authAuditLogRepository;

    @Data
    @Schema(description = "TOTP 2FA Provisioning details with Base32 secret and QR Code Data URI")
    public static class TotpSetupResponse {
        @Schema(description = "Base32 encoded TOTP shared secret", example = "JBSWY3DPEHPK3PXP")
        private String secret;

        @Schema(description = "RFC 6238 otpauth:// QR code data URI for Google Authenticator", example = "data:image/png;base64,iVBORw0KGgo...")
        private String qrCodeUri;
    }

    @Data
    @Schema(description = "Payload to verify a 6-digit TOTP authentication code")
    public static class TotpVerifyRequest {
        @Schema(description = "User TOTP secret", example = "JBSWY3DPEHPK3PXP")
        private String secret;

        @Schema(description = "6-digit time-based code", example = "492817")
        private String code;
    }

    /**
     * Register a new user in Keycloak IAM and PostgreSQL database.
     */
    @Operation(
            summary = "Register new user account",
            description = "Provisions a new user account across Keycloak IAM realm and PostgreSQL user_auth_db, " +
                    "assigns realm/client roles, seeds credentials, and sets up optional HIPAA TOTP 2FA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered in Keycloak and PostgreSQL",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists in Keycloak IAM",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
    @Operation(
            summary = "Authenticate user (Direct Access Grant)",
            description = "Exchanges username/password credentials against Keycloak 24 IAM for a signed RS256 JWT access token " +
                    "and refresh token. Enforces TOTP 2FA verification when configured on user profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful, returns tokens and profile",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or TOTP verification failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Account disabled, locked, or role unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
    @Operation(
            summary = "Keycloak Single Sign-Out (Logout)",
            description = "Revokes the active refresh token and terminates the user's Keycloak session across the realm.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session terminated and token revoked"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
    @Operation(
            summary = "Refresh JWT access token",
            description = "Uses Keycloak OpenID Connect refresh_token grant to rotate tokens and generate a fresh RS256 access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Expired or invalid refresh token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
    @Operation(
            summary = "Get current authenticated user profile",
            description = "Decodes active Keycloak JWT access token and returns subject, claims, expiration, and realm roles.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user token claims and identity metadata"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid Bearer JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
    @Operation(
            summary = "Generate TOTP 2FA setup secret and QR code",
            description = "Generates a cryptographically secure RFC 6238 Base32 secret key and otpauth:// URI for authenticator app enrollment.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TOTP secret and QR code URI generated",
                    content = @Content(schema = @Schema(implementation = TotpSetupResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
    @Operation(
            summary = "Verify 6-digit TOTP code",
            description = "Validates submitted 6-digit time-based code against the provided secret with clock-drift windowing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TOTP verification result and HIPAA compliance status")
    })
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
    @Operation(
            summary = "Retrieve HIPAA security audit logs",
            description = "Queries the immutable PostgreSQL audit log for authentication events, login failures, role updates, and token lifecycle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of recent HIPAA audit entries",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthAuditLog.class)))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN realm role",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(authAuditLogRepository.findTop50ByOrderByTimestampDesc());
    }

    /**
     * Admin verification of physician medical license.
     */
    @Operation(
            summary = "Verify physician medical license",
            description = "Enables hospital credentialing administrators to verify and approve a physician's clinical license status.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor profile verified and active",
                    content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "404", description = "Doctor profile not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN realm role",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/doctor/verify-license")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorProfile> verifyDoctorLicense(
            @Parameter(description = "Physician Doctor ID or User UUID", example = "usr-doc-204")
            @RequestParam("doctorId") String doctorId,
            @AuthenticationPrincipal Jwt jwt) {
        String adminId = jwt.getSubject();
        return doctorVerificationService.verifyDoctor(doctorId, adminId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

