package com.healthcare.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration providing Keycloak Admin Client SDK instance.
 * Uses Client Credentials (service-account) flow against Keycloak 24 IAM.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminClientConfig {

    private final KeycloakAdminConfigProperties properties;

    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak Admin Client for server: {}, realm: {}, clientId: {}",
                properties.getServerUrl(), properties.getRealm(), properties.getClientId());

        KeycloakBuilder builder = KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getClientId())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientSecret(properties.getClientSecret());

        // Support password grant fallback if admin credentials are provided
        if (properties.getAdminUsername() != null && !properties.getAdminUsername().isBlank()) {
            log.info("Configuring Keycloak Admin Client with Master Admin Credentials");
            builder.realm(properties.getMasterRealm() != null ? properties.getMasterRealm() : "master")
                   .clientId("admin-cli")
                   .grantType(OAuth2Constants.PASSWORD)
                   .username(properties.getAdminUsername())
                   .password(properties.getAdminPassword());
        }

        Keycloak keycloak = builder.build();
        log.info("Keycloak Admin Client successfully initialized.");
        return keycloak;
    }
}
