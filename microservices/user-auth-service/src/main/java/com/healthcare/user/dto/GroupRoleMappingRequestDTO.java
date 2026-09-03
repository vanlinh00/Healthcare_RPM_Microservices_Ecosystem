package com.healthcare.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for mapping or unmapping Keycloak realm/client roles to a specific user group")
public class GroupRoleMappingRequestDTO {

    /**
     * Keycloak Group ID.
     */
    @Schema(description = "Keycloak Group UUID", example = "grp-doc-writers-10", requiredMode = Schema.RequiredMode.REQUIRED)
    private String groupId;

    /**
     * List of role names to assign or revoke.
     */
    @NotEmpty(message = "At least one role name must be provided")
    @Schema(description = "List of role names to assign to group", example = "[\"DOCTOR\", \"PRESCRIPTION_WRITER\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> roleNames;

    /**
     * True if roles belong to a specific Client rather than Realm level.
     */
    @Builder.Default
    @Schema(description = "Whether the roles are client-level roles", example = "false")
    private boolean clientRole = false;

    /**
     * Client ID or Client UUID (required if clientRole is true).
     */
    @Schema(description = "Keycloak Client identifier (if clientRole is true)", example = "healthcare-api-gateway")
    private String clientId;
}

