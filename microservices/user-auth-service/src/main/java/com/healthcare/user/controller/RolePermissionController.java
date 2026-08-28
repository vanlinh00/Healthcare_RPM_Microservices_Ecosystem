package com.healthcare.user.controller;

import com.healthcare.user.dto.*;
import com.healthcare.user.service.KeycloakRolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Keycloak 24 IAM Role, Permission, and Composite Hierarchy Management.
 */
@RestController
@RequestMapping("/api/v1/iam")
@RequiredArgsConstructor
@Slf4j
public class RolePermissionController {

    private final KeycloakRolePermissionService rolePermissionService;

    // ==========================================
    // 1. Role CRUD Endpoints
    // ==========================================

    /**
     * Create a new Realm-level or Client-level role.
     */
    @PostMapping("/roles")
    public ResponseEntity<RoleResponseDTO> createRole(@Valid @RequestBody RoleRequestDTO request) {
        log.info("REST: Request to create role '{}'", request.getName());
        RoleResponseDTO createdRole = rolePermissionService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    /**
     * Fetch all Realm-level roles or search with an optional query prefix.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> getRealmRoles(
            @RequestParam(required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(rolePermissionService.searchRoles(query, false, null));
        }
        return ResponseEntity.ok(rolePermissionService.getAllRealmRoles());
    }

    /**
     * Fetch all Client-level roles for a given client ID.
     */
    @GetMapping("/roles/client/{clientId}")
    public ResponseEntity<List<RoleResponseDTO>> getClientRoles(
            @PathVariable String clientId,
            @RequestParam(required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(rolePermissionService.searchRoles(query, true, clientId));
        }
        return ResponseEntity.ok(rolePermissionService.getAllClientRoles(clientId));
    }

    /**
     * Get details for a specific role.
     */
    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponseDTO> getRoleByName(
            @PathVariable String roleName,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        return ResponseEntity.ok(rolePermissionService.getRoleByName(roleName, clientRole, clientId));
    }

    /**
     * Update an existing role's description, attributes, or composite structure.
     */
    @PutMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponseDTO> updateRole(
            @PathVariable String roleName,
            @Valid @RequestBody RoleRequestDTO request) {
        log.info("REST: Request to update role '{}'", roleName);
        return ResponseEntity.ok(rolePermissionService.updateRole(roleName, request));
    }

    /**
     * Delete a role safely from Keycloak.
     */
    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<Map<String, String>> deleteRole(
            @PathVariable String roleName,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        log.info("REST: Request to delete role '{}'", roleName);
        rolePermissionService.deleteRole(roleName, clientRole, clientId);
        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "message", "Role '" + roleName + "' successfully deleted from Keycloak."
        ));
    }

    // ==========================================
    // 2. Composite Roles Endpoints
    // ==========================================

    /**
     * Add sub-roles to an existing parent composite role.
     */
    @PostMapping("/roles/{roleName}/composites")
    public ResponseEntity<RoleResponseDTO> addSubRolesToComposite(
            @PathVariable String roleName,
            @RequestBody List<String> subRoleNames,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        log.info("REST: Adding sub-roles {} to parent role '{}'", subRoleNames, roleName);
        RoleResponseDTO updatedRole = rolePermissionService.addSubRolesToComposite(roleName, subRoleNames, clientRole, clientId);
        return ResponseEntity.ok(updatedRole);
    }

    /**
     * Remove sub-roles from a composite role.
     */
    @DeleteMapping("/roles/{roleName}/composites")
    public ResponseEntity<RoleResponseDTO> removeSubRolesFromComposite(
            @PathVariable String roleName,
            @RequestBody List<String> subRoleNames,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        log.info("REST: Removing sub-roles {} from parent role '{}'", subRoleNames, roleName);
        RoleResponseDTO updatedRole = rolePermissionService.removeSubRolesFromComposite(roleName, subRoleNames, clientRole, clientId);
        return ResponseEntity.ok(updatedRole);
    }

    // ==========================================
    // 3. User & Group Role Mappings Endpoints
    // ==========================================

    /**
     * Assign Realm or Client roles to a specific user.
     */
    @PostMapping("/mappings/users")
    public ResponseEntity<Map<String, Object>> assignRolesToUser(@Valid @RequestBody UserRoleMappingRequestDTO request) {
        log.info("REST: Assigning roles {} to user '{}'", request.getRoleNames(), request.getUserId());
        rolePermissionService.assignRolesToUser(request);
        return ResponseEntity.ok(Map.of(
                "status", "ASSIGNED",
                "userId", request.getUserId(),
                "roles", request.getRoleNames()
        ));
    }

    /**
     * Revoke Realm or Client roles from a specific user.
     */
    @DeleteMapping("/mappings/users")
    public ResponseEntity<Map<String, Object>> revokeRolesFromUser(@Valid @RequestBody UserRoleMappingRequestDTO request) {
        log.info("REST: Revoking roles {} from user '{}'", request.getRoleNames(), request.getUserId());
        rolePermissionService.revokeRolesFromUser(request);
        return ResponseEntity.ok(Map.of(
                "status", "REVOKED",
                "userId", request.getUserId(),
                "roles", request.getRoleNames()
        ));
    }

    /**
     * Assign Realm or Client roles to a specific group.
     */
    @PostMapping("/mappings/groups")
    public ResponseEntity<Map<String, Object>> assignRolesToGroup(@Valid @RequestBody GroupRoleMappingRequestDTO request) {
        log.info("REST: Assigning roles {} to group '{}'", request.getRoleNames(), request.getGroupId());
        rolePermissionService.assignRolesToGroup(request);
        return ResponseEntity.ok(Map.of(
                "status", "ASSIGNED",
                "groupId", request.getGroupId(),
                "roles", request.getRoleNames()
        ));
    }

    /**
     * Revoke Realm or Client roles from a specific group.
     */
    @DeleteMapping("/mappings/groups")
    public ResponseEntity<Map<String, Object>> revokeRolesFromGroup(@Valid @RequestBody GroupRoleMappingRequestDTO request) {
        log.info("REST: Revoking roles {} from group '{}'", request.getRoleNames(), request.getGroupId());
        rolePermissionService.revokeRolesFromGroup(request);
        return ResponseEntity.ok(Map.of(
                "status", "REVOKED",
                "groupId", request.getGroupId(),
                "roles", request.getRoleNames()
        ));
    }

    /**
     * Permission Auditing: Retrieve direct, client, and composite effective roles for a user.
     */
    @GetMapping("/mappings/users/{userId}/effective")
    public ResponseEntity<EffectiveUserPermissionsDTO> getEffectiveUserPermissions(@PathVariable String userId) {
        log.info("REST: Auditing effective permissions for user '{}'", userId);
        return ResponseEntity.ok(rolePermissionService.getEffectiveUserPermissions(userId));
    }
}
