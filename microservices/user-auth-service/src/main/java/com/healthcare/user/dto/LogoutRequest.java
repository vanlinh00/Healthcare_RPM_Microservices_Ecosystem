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
@Schema(description = "Payload for terminating Keycloak user session and revoking active refresh tokens")
public class LogoutRequest {

    @NotBlank(message = "Refresh token is required for Keycloak session revocation")
    @JsonProperty("refresh_token")
    @Schema(description = "Active refresh token to revoke", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    @JsonProperty("all_sessions")
    @Builder.Default
    @Schema(description = "If true, terminates all active sessions across all devices for this user in Keycloak", example = "false")
    private boolean allSessions = false;
}

