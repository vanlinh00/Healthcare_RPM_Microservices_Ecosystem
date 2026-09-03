package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representation of a Keycloak realm or client role, including composite sub-roles and attributes")
public class RoleResponseDTO {

    @Schema(description = "Role unique UUID in Keycloak", example = "role-uuid-101")
    private String id;

    @Schema(description = "Role name", example = "DOCTOR")
    private String name;

    @Schema(description = "Role description", example = "Attending Physician with clinical permissions")
    private String description;

    @Schema(description = "Whether the role is composite", example = "true")
    private boolean composite;

    @Schema(description = "Whether the role is client-level", example = "false")
    private boolean clientRole;

    @Schema(description = "Container ID (realm name or client UUID)", example = "healthcare-realm")
    private String containerId;

    @Schema(description = "Client name if client-level role", example = "healthcare-api-gateway")
    private String clientName;

    @Schema(description = "Custom HIPAA metadata attributes")
    private Map<String, List<String>> attributes;

    @Schema(description = "List of direct sub-roles if this role is composite")
    private List<RoleResponseDTO> compositeSubRoles;
}

