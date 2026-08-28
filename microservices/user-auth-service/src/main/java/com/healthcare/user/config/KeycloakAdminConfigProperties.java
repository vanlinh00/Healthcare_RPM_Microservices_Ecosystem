package com.healthcare.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
@Data
public class KeycloakAdminConfigProperties {
    /**
     * Keycloak Auth Server Base URL (e.g., http://localhost:8080)
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * Target Realm for Healthcare RPM (e.g., healthcare-realm)
     */
    private String realm = "healthcare-realm";

    /**
     * Admin Client ID (e.g., healthcare-api-gateway or admin-cli)
     */
    private String clientId = "healthcare-api-gateway";

    /**
     * Client Secret for Client Credentials (Service Account) flow
     */
    private String clientSecret = "hExghH54qXgHTl3WnNJJ1nCW2dxeRBI1";

    /**
     * Optional Master Realm for cross-realm admin management
     */
    private String masterRealm = "master";

    /**
     * Optional Master Admin username fallback
     */
    private String adminUsername = "admin";

    /**
     * Optional Master Admin password fallback
     */
    private String adminPassword = "adminpassword";

    /**
     * Connection pool size
     */
    private int connectionPoolSize = 20;
}
