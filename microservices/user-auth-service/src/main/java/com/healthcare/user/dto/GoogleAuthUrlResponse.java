package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keycloak Google Identity Provider authorization URL payload")
public class GoogleAuthUrlResponse {

    @JsonProperty("auth_url")
    @Schema(description = "Direct Keycloak OIDC authorization URL with kc_idp_hint=google", example = "http://localhost:8080/realms/healthcare-realm/protocol/openid-connect/auth?client_id=healthcare-api-gateway&response_type=code&scope=openid%20profile%20email%20roles&redirect_uri=http://localhost:3000/auth/callback&kc_idp_hint=google")
    private String authUrl;

    @JsonProperty("keycloak_broker_endpoint")
    @Schema(description = "Keycloak broker redirect URI to register in Google Cloud Console Credentials", example = "http://localhost:8080/realms/healthcare-realm/broker/google/endpoint")
    private String keycloakBrokerEndpoint;

    @JsonProperty("client_id")
    @Schema(description = "Keycloak Client ID used for OIDC authentication", example = "healthcare-api-gateway")
    private String clientId;

    @JsonProperty("realm")
    @Schema(description = "Keycloak Realm name", example = "healthcare-realm")
    private String realm;

    @JsonProperty("provider")
    @Schema(description = "Identity provider alias in Keycloak", example = "google")
    private String provider;

    @JsonProperty("redirect_uri")
    @Schema(description = "Configured client application redirect callback URI", example = "http://localhost:3000/auth/callback")
    private String redirectUri;
}
