package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "Request payload to exchange Keycloak authorization code (after Google social login) for JWT access tokens")
public class GoogleAuthCodeRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "Authorization code returned by Keycloak after Google authentication", example = "91a18274-c089-4cb3-911e-08991be249a1.d8e01")
    private String code;

    @JsonProperty("redirect_uri")
    @Schema(description = "Callback URI originally passed when initiating the Google OAuth flow", example = "http://localhost:3000/auth/callback")
    private String redirectUri;

    @Schema(description = "Client device UUID", example = "dev-ios-9410")
    private String deviceId;

    @Schema(description = "Remote client IP address", example = "192.168.1.100")
    private String clientIp;

    @Schema(description = "Remote client user agent", example = "Mozilla/5.0")
    private String userAgent;
}
