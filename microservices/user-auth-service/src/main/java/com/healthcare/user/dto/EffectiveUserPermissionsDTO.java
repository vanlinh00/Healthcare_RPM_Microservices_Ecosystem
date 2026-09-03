package com.healthcare.user.dto;

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
@Schema(description = "Deep audit representation of all direct and hierarchical composite permissions assigned to a user")
public class EffectiveUserPermissionsDTO {

    @Schema(description = "Keycloak user UUID", example = "usr-doc-204")
    private String userId;

    @Schema(description = "User login username", example = "doctor_emily")
    private String username;

    @Schema(description = "User primary email", example = "emily.vance@healthcare.org")
    private String email;

    @Schema(description = "Directly assigned Realm-level roles", example = "[\"DOCTOR\"]")
    private List<String> directRealmRoles;

    @Schema(description = "Full composite expanded Realm-level roles inherited via nesting", example = "[\"DOCTOR\", \"READ_PATIENT_VITALS\", \"WRITE_PRESCRIPTIONS\"]")
    private List<String> compositeEffectiveRealmRoles;

    @Schema(description = "Directly assigned Client-level roles grouped by client identifier")
    private Map<String, List<String>> directClientRoles;

    @Schema(description = "Full composite expanded Client-level roles grouped by client identifier")
    private Map<String, List<String>> compositeEffectiveClientRoles;

    @Schema(description = "Total count of unique effective roles across all scopes", example = "7")
    private int totalEffectiveRolesCount;
}

