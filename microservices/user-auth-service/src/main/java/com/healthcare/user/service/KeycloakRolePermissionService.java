package com.healthcare.user.service;

import com.healthcare.user.dto.*;

import java.util.List;

/**
 * Service interface for managing Keycloak Roles, Permissions, Composite Hierarchies,
 * and User/Group Role Mappings via the Keycloak Admin Client SDK.
 */
public interface KeycloakRolePermissionService {

    /**
     * Create a new Realm-level or Client-level role.
     */
    RoleResponseDTO createRole(RoleRequestDTO request);

    /**
     * Fetch all Realm-level roles.
     */
    List<RoleResponseDTO> getAllRealmRoles();

    /**
     * Fetch all Client-level roles for a specific client.
     */
    List<RoleResponseDTO> getAllClientRoles(String clientId);

    /**
     * Search roles by prefix/query.
     */
    List<RoleResponseDTO> searchRoles(String query, boolean clientRole, String clientId);

    /**
     * Get details of a specific role by name.
     */
    RoleResponseDTO getRoleByName(String roleName, boolean clientRole, String clientId);

    /**
     * Update an existing role's description, attributes, and composition.
     */
    RoleResponseDTO updateRole(String roleName, RoleRequestDTO request);

    /**
     * Delete a role safely by name.
     */
    void deleteRole(String roleName, boolean clientRole, String clientId);

    /**
     * Associate sub-roles to a parent role to make it composite.
     */
    RoleResponseDTO addSubRolesToComposite(String parentRoleName, List<String> subRoleNames, boolean clientRole, String clientId);

    /**
     * Remove sub-roles from a composite role.
     */
    RoleResponseDTO removeSubRolesFromComposite(String parentRoleName, List<String> subRoleNames, boolean clientRole, String clientId);

    /**
     * Assign realm or client roles to a specific user.
     */
    void assignRolesToUser(UserRoleMappingRequestDTO request);

    /**
     * Revoke realm or client roles from a specific user.
     */
    void revokeRolesFromUser(UserRoleMappingRequestDTO request);

    /**
     * Assign realm or client roles to a specific group.
     */
    void assignRolesToGroup(GroupRoleMappingRequestDTO request);

    /**
     * Revoke realm or client roles from a specific group.
     */
    void revokeRolesFromGroup(GroupRoleMappingRequestDTO request);

    /**
     * Audit and retrieve all direct, client, and composite effective roles for a user.
     */
    EffectiveUserPermissionsDTO getEffectiveUserPermissions(String userId);
}
