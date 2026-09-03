package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 / OIDC Direct Access Grant Authentication response carrying Keycloak JWT access token, refresh token, and user profile")
public class LoginResponse {

    @JsonProperty("access_token")
    @Schema(description = "Signed RS256 JWT access token containing realm roles and group claims", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(description = "OIDC refresh token for token renewal and session revocation", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @JsonProperty("token_type")
    @Builder.Default
    @Schema(description = "OAuth2 token type identifier", example = "Bearer")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @Schema(description = "Access token lifespan in seconds", example = "1800")
    private Long expiresIn;

    @JsonProperty("refresh_expires_in")
    @Schema(description = "Refresh token lifespan in seconds", example = "18000")
    private Long refreshExpiresIn;

    @JsonProperty("session_state")
    @Schema(description = "Keycloak active session UUID", example = "d9418a00-1123-41c9-82a1-0081bf44ef1a")
    private String sessionState;

    @Schema(description = "OAuth2 granted scopes", example = "openid email profile healthcare-api roles")
    private String scope;

    @JsonProperty("totp_required")
    @Schema(description = "Indicates whether 2FA TOTP is mandated for this account", example = "false")
    private boolean totpRequired;

    @JsonProperty("totp_verified")
    @Schema(description = "Indicates whether TOTP 2FA was verified successfully during authentication", example = "true")
    private boolean totpVerified;

    @Schema(description = "User profile and role identity")
    private UserProfileDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Authenticated user profile summary")
    public static class UserProfileDto {
        @Schema(description = "User unique UUID", example = "usr-doc-204")
        private String id;

        @Schema(description = "User registered email", example = "emily.vance@healthcare.org")
        private String email;

        @Schema(description = "User account username", example = "doctor_emily")
        private String username;

        @Schema(description = "First name", example = "Emily")
        private String firstName;

        @Schema(description = "Last name", example = "Vance, MD")
        private String lastName;

        @Schema(description = "Primary assigned role", example = "DOCTOR")
        private String primaryRole;

        @Schema(description = "List of all assigned Keycloak realm and client roles", example = "[\"DOCTOR\", \"default-roles-healthcare\"]")
        private List<String> roles;

        @Schema(description = "Whether TOTP 2FA is active on the account", example = "true")
        private boolean totpEnabled;

        @Schema(description = "Account enabled status", example = "true")
        private boolean active;

        @Schema(description = "Custom profile attributes and metadata")
        private Map<String, Object> metadata;
    }
}

