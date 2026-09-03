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
@Schema(description = "Keycloak Google Identity Provider configuration metadata and setup instructions")
public class GoogleIdpConfigResponse {

    @Schema(description = "Whether Google Identity Provider is configured in Keycloak", example = "true")
    private boolean configured;

    @JsonProperty("identity_provider_alias")
    @Schema(description = "Keycloak Identity Provider Alias", example = "google")
    private String identityProviderAlias;

    @JsonProperty("keycloak_broker_redirect_uri")
    @Schema(description = "Redirect URI to configure in Google Cloud Console Credentials -> Authorized Redirect URIs", example = "http://localhost:8080/realms/healthcare-realm/broker/google/endpoint")
    private String keycloakBrokerRedirectUri;

    @JsonProperty("client_callback_url")
    @Schema(description = "Client web application callback URL", example = "http://localhost:3000/auth/callback")
    private String clientCallbackUrl;

    @JsonProperty("setup_instructions")
    @Schema(description = "Keycloak & Google Cloud Console setup instructions")
    private List<String> setupInstructions;

    @Schema(description = "Additional configuration metadata")
    private Map<String, Object> metadata;
}
