package com.healthcare.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Payload for creating or updating a Keycloak realm or client role with optional composite inheritance and HIPAA attributes")
public class RoleRequestDTO {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Role name can only contain alphanumeric characters, underscores, and hyphens")
    @Schema(description = "Unique role identifier name", example = "DOCTOR", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed role description and clinical purpose", example = "Attending physician with vitals and prescribing authority")
    private String description;

    @Builder.Default
    @Schema(description = "Whether this role is a composite role that inherits sub-roles", example = "true")
    private boolean composite = false;

    /**
     * Optional sub-role names to associate if this is a composite role.
     */
    @Schema(description = "Sub-role names nested inside this composite role", example = "[\"READ_PATIENT_VITALS\", \"WRITE_PRESCRIPTIONS\"]")
    private List<String> subRoleNames;

    /**
     * Whether this is a client-level role (defaults to false for realm-level role).
     */
    @Builder.Default
    @Schema(description = "Flag indicating client-scoped role vs realm-wide role", example = "false")
    private boolean clientRole = false;

    /**
     * Client UUID or Client ID if clientRole is true (e.g., healthcare-api-gateway).
     */
    @Schema(description = "Target Keycloak client identifier (required if clientRole is true)", example = "healthcare-api-gateway")
    private String clientId;

    /**
     * Custom HIPAA / RBAC metadata attributes (e.g., clearance_level, department).
     */
    @Schema(description = "Custom HIPAA security metadata attributes")
    private Map<String, List<String>> attributes;
}

