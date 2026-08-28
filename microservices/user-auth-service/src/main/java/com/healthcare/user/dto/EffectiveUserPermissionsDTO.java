package com.healthcare.user.dto;

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
public class EffectiveUserPermissionsDTO {

    private String userId;
    private String username;
    private String email;
    private List<String> directRealmRoles;
    private List<String> compositeEffectiveRealmRoles;
    private Map<String, List<String>> directClientRoles;
    private Map<String, List<String>> compositeEffectiveClientRoles;
    private int totalEffectiveRolesCount;
}
