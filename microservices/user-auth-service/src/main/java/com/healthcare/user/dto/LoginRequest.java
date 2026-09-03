package com.healthcare.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for user authentication with Keycloak Direct Access Grants and optional TOTP 2FA")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or registered email address", example = "doctor_emily", requiredMode = Schema.RequiredMode.REQUIRED)
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * Optional 6-digit TOTP 2FA code (required if user has 2FA enabled).
     */
    @Schema(description = "Optional 6-digit TOTP code (required for 2FA-enforced accounts)", example = "492817")
    private String totpCode;

    @Schema(description = "Client device UUID for session tracking", example = "dev-ios-9410")
    private String deviceId;

    @Schema(description = "Remote client IP address", example = "192.168.1.100")
    private String clientIp;

    @Schema(description = "Client browser or application user agent", example = "Mozilla/5.0")
    private String userAgent;
}

