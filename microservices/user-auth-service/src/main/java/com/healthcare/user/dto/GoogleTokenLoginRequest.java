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
@Schema(description = "Payload for Google OAuth2 / OIDC Token login via Keycloak token exchange or federated credential verification")
public class GoogleTokenLoginRequest {

    @NotBlank(message = "Google ID token or access token is required")
    @JsonProperty("id_token")
    @Schema(description = "Google signed JWT ID token from Google Identity Services", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjAwZDY...")
    private String idToken;

    @JsonProperty("access_token")
    @Schema(description = "Optional Google OAuth2 access token", example = "ya29.a0AWY7Ckk...")
    private String accessToken;

    @Schema(description = "Client device UUID", example = "dev-browser-client")
    private String deviceId;

    @Schema(description = "Remote client IP address", example = "192.168.1.100")
    private String clientIp;

    @Schema(description = "Remote client user agent", example = "Mozilla/5.0")
    private String userAgent;
}
