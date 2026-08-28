package com.healthcare.user.dto;

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
public class RoleRequestDTO {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Role name can only contain alphanumeric characters, underscores, and hyphens")
    private String name;

    private String description;

    @Builder.Default
    private boolean composite = false;

    /**
     * Optional sub-role names to associate if this is a composite role.
     */
    private List<String> subRoleNames;

    /**
     * Whether this is a client-level role (defaults to false for realm-level role).
     */
    @Builder.Default
    private boolean clientRole = false;

    /**
     * Client UUID or Client ID if clientRole is true (e.g., healthcare-api-gateway).
     */
    private String clientId;

    /**
     * Custom HIPAA / RBAC metadata attributes (e.g., clearance_level, department).
     */
    private Map<String, List<String>> attributes;
}
