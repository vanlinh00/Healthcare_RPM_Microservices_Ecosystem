package com.healthcare.user.dto;

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
public class UserRoleMappingRequestDTO {

    /**
     * User ID (UUID from Keycloak).
     */
    private String userId;

    /**
     * List of role names to assign or revoke.
     */
    @NotEmpty(message = "At least one role name must be provided")
    private List<String> roleNames;

    /**
     * True if roles belong to a specific Client rather than Realm level.
     */
    @Builder.Default
    private boolean clientRole = false;

    /**
     * Client ID or Client UUID (required if clientRole is true).
     */
    private String clientId;
}
