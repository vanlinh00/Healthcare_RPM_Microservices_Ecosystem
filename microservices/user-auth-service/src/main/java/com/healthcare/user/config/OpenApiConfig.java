package com.healthcare.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 Platform Specification Configuration for User & IAM Microservice.
 * Integrates Swagger UI, Keycloak OAuth2 / OIDC JWT security definitions,
 * HIPAA compliance metadata, and service grouping.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${server.port:8081}") String serverPort,
            @Value("${keycloak.realm:healthcare-realm}") String realm) {

        return new OpenAPI()
                .info(new Info()
                        .title("Healthcare & RPM User & IAM Authentication Microservice API")
                        .version("1.0.0")
                        .description("### Enterprise-Grade Healthcare & Remote Patient Monitoring (RPM) Identity Platform\n\n" +
                                "This microservice provides identity lifecycle, Keycloak 24 IAM management, OAuth2 / OIDC Direct Access Grants, " +
                                "HIPAA-compliant RFC 6238 TOTP Multi-Factor Authentication (2FA), Role-Based Access Control (RBAC), " +
                                "composite role permission hierarchies, and physician verification.\n\n" +
                                "#### Key Security & Architecture Features:\n" +
                                "- **Keycloak 24 IAM Integration**: Token issuance, refresh token rotation, single sign-out.\n" +
                                "- **HIPAA Security Rule Enforcement**: Dual-factor TOTP for high-privilege roles, audit logging.\n" +
                                "- **Composite Role Mapping**: Deep hierarchical permission resolution for Doctors, Nurses, Patients, Pharmacists, and Admins.\n" +
                                "- **Group Membership**: Token claims embedding hierarchical paths such as `/Doctors-Writers`.")
                        .contact(new Contact()
                                .name("Healthcare RPM Engineering & Security Team")
                                .email("linhnv03@ominext.com")
                                .url("https://healthcare.ecosystem.org"))
                        .license(new License()
                                .name("HIPAA Audited & Apache 2.0")
                                .url("https://www.hhs.gov/hipaa/for-professionals/security/index.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Direct Local Microservice Runtime (Spring Boot 3.4)"),
                        new Server()
                                .url("http://localhost:8000/user-service")
                                .description("Kong API Gateway Ingress Proxy Routing"),
                        new Server()
                                .url("https://ais-dev-fyrk2325w22brqu3wlumc6-64171391955.asia-southeast1.run.app")
                                .description("Cloud Run Production/Dev Environment")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Authentication & MFA")
                                .description("Keycloak 24 OIDC authentication, registration, token refresh, single sign-out, and RFC 6238 TOTP 2FA"),
                        new Tag()
                                .name("Keycloak IAM & RBAC")
                                .description("Keycloak realm & client role lifecycle, composite permission hierarchies, and user/group mappings"),
                        new Tag()
                                .name("Physician Verification & Compliance")
                                .description("Physician medical license verification and HIPAA audit log telemetry")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter Keycloak JWT Access Token obtained from `/api/v1/auth/login`. " +
                                        "Token carries standard claims (`sub`, `preferred_username`, `realm_access.roles`, `groups`).")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
