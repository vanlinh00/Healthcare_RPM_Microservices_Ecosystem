package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class LoginResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

    @JsonProperty("session_state")
    private String sessionState;

    private String scope;

    @JsonProperty("totp_required")
    private boolean totpRequired;

    @JsonProperty("totp_verified")
    private boolean totpVerified;

    private UserProfileDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileDto {
        private String id;
        private String email;
        private String username;
        private String firstName;
        private String lastName;
        private String primaryRole;
        private List<String> roles;
        private boolean totpEnabled;
        private boolean active;
        private Map<String, Object> metadata;
    }
}
