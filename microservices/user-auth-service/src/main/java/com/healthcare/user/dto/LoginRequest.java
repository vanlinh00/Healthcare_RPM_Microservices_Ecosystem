package com.healthcare.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional 6-digit TOTP 2FA code (required if user has 2FA enabled).
     */
    private String totpCode;

    private String deviceId;
    private String clientIp;
    private String userAgent;
}
