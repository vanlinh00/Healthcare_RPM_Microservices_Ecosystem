package com.healthcare.user.controller;

import com.healthcare.user.dto.*;
import com.healthcare.user.exception.ApiErrorResponse;
import com.healthcare.user.service.KeycloakRolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Keycloak IAM & RBAC", description = "Keycloak 24 realm & client role lifecycle, composite permission hierarchies, and user/group role mappings")
@SecurityRequirement(name = "bearerAuth")
public class RolePermissionController {

    private final KeycloakRolePermissionService rolePermissionService;

    // ==========================================
    // 1. Role CRUD Endpoints
    // ==========================================

    /**
     * Create a new Realm-level or Client-level role.
     */
    @Operation(
            summary = "Create Realm or Client role",
            description = "Creates a new role within the Keycloak realm or associated with a specific client (e.g., healthcare-api-gateway). " +
                    "Optionally creates it as a composite role with sub-roles and custom HIPAA metadata attributes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role successfully created in Keycloak",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role request or validation failure",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Role already exists in Keycloak realm",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/roles")
    public ResponseEntity<RoleResponseDTO> createRole(@Valid @RequestBody RoleRequestDTO request) {
        log.info("REST: Request to create role '{}'", request.getName());
        RoleResponseDTO createdRole = rolePermissionService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    /**
     * Fetch all Realm-level roles or search with an optional query prefix.
     */
    @Operation(
            summary = "List all realm-level roles",
            description = "Returns all realm roles defined in Keycloak with their composite indicators, descriptions, and metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of realm roles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResponseDTO.class))))
    })
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> getRealmRoles(
            @Parameter(description = "Optional role name search prefix", example = "DOC")
            @RequestParam(name = "query", required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(rolePermissionService.searchRoles(query, false, null));
        }
        return ResponseEntity.ok(rolePermissionService.getAllRealmRoles());
    }

    /**
     * Fetch all Client-level roles for a given client ID.
     */
    @Operation(
            summary = "List all client-level roles",
            description = "Returns all client-scoped roles belonging to the designated Keycloak client container (e.g. healthcare-api-gateway)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of client roles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Client ID not found in Keycloak",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/roles/client/{clientId}")
    public ResponseEntity<List<RoleResponseDTO>> getClientRoles(
            @Parameter(description = "Client UUID or Client identifier", example = "healthcare-api-gateway")
            @PathVariable("clientId") String clientId,
            @Parameter(description = "Optional role search prefix")
            @RequestParam(name = "query", required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(rolePermissionService.searchRoles(query, true, clientId));
        }
        return ResponseEntity.ok(rolePermissionService.getAllClientRoles(clientId));
    }

    /**
     * Get details for a specific role.
     */
    @Operation(
            summary = "Get role details by name",
            description = "Fetches complete role representation, including whether it is composite, its attributes, and direct sub-roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role details retrieved",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponseDTO> getRoleByName(
            @Parameter(description = "Role name", example = "DOCTOR")
            @PathVariable("roleName") String roleName,
            @Parameter(description = "Whether the role is a client-level role", example = "false")
            @RequestParam(name = "clientRole", defaultValue = "false") boolean clientRole,
            @Parameter(description = "Client ID if clientRole is true", example = "healthcare-api-gateway")
            @RequestParam(name = "clientId", required = false) String clientId) {
        return ResponseEntity.ok(rolePermissionService.getRoleByName(roleName, clientRole, clientId));
    }

    /**
     * Update an existing role's description, attributes, or composite structure.
     */
    @Operation(
            summary = "Update role definition",
            description = "Updates an existing Keycloak role description and custom attributes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponseDTO> updateRole(
            @Parameter(description = "Role name to update", example = "DOCTOR")
            @PathVariable("roleName") String roleName,
            @Valid @RequestBody RoleRequestDTO request) {
        log.info("REST: Request to update role '{}'", roleName);
        return ResponseEntity.ok(rolePermissionService.updateRole(roleName, request));
    }

    /**
     * Delete a role safely from Keycloak.
     */
    @Operation(
            summary = "Delete role",
            description = "Permanently deletes a role from Keycloak realm or client container."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Role not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<Map<String, String>> deleteRole(
            @Parameter(description = "Role name to delete", example = "TEMPORARY_STAFF")
            @PathVariable("roleName") String roleName,
            @Parameter(description = "Whether the role is client-scoped", example = "false")
            @RequestParam(name = "clientRole", defaultValue = "false") boolean clientRole,
            @Parameter(description = "Client ID if client-scoped", example = "healthcare-api-gateway")
            @RequestParam(name = "clientId", required = false) String clientId) {
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
    @Operation(
            summary = "Add sub-roles to composite role",
            description = "Enables hierarchical permission inheritance by nesting child roles into a parent composite role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sub-roles successfully added to composite",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Parent or sub-role not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/roles/{roleName}/composites")
    public ResponseEntity<RoleResponseDTO> addSubRolesToComposite(
            @Parameter(description = "Parent composite role name", example = "DOCTOR")
            @PathVariable("roleName") String roleName,
            @Parameter(description = "List of sub-role names to nest", example = "[\"READ_PATIENT_VITALS\", \"WRITE_PRESCRIPTIONS\"]")
            @RequestBody List<String> subRoleNames,
            @RequestParam(name = "clientRole", defaultValue = "false") boolean clientRole,
            @RequestParam(name = "clientId", required = false) String clientId) {
        log.info("REST: Adding sub-roles {} to parent role '{}'", subRoleNames, roleName);
        RoleResponseDTO updatedRole = rolePermissionService.addSubRolesToComposite(roleName, subRoleNames, clientRole, clientId);
        return ResponseEntity.ok(updatedRole);
    }

    /**
     * Remove sub-roles from a composite role.
     */
    @Operation(
            summary = "Remove sub-roles from composite role",
            description = "Removes specified sub-roles from the composite hierarchy of a parent role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sub-roles successfully detached from composite",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Parent or sub-role not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/roles/{roleName}/composites")
    public ResponseEntity<RoleResponseDTO> removeSubRolesFromComposite(
            @Parameter(description = "Parent composite role name", example = "DOCTOR")
            @PathVariable("roleName") String roleName,
            @Parameter(description = "List of sub-role names to remove", example = "[\"TEMPORARY_OVERRIDE\"]")
            @RequestBody List<String> subRoleNames,
            @RequestParam(name = "clientRole", defaultValue = "false") boolean clientRole,
            @RequestParam(name = "clientId", required = false) String clientId) {
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
    @Operation(
            summary = "Assign roles to user",
            description = "Directly grants realm or client roles to a designated Keycloak user identity."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles successfully assigned to user")
    })
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
    @Operation(
            summary = "Revoke roles from user",
            description = "Revokes realm or client roles previously assigned to a Keycloak user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles successfully revoked from user")
    })
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
    @Operation(
            summary = "Assign roles to group",
            description = "Grants realm or client roles to a Keycloak group (e.g. /Doctors-Writers), propagating permissions to all members."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles successfully assigned to group")
    })
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
    @Operation(
            summary = "Revoke roles from group",
            description = "Revokes realm or client roles mapped to a Keycloak group."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles successfully revoked from group")
    })
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
    @Operation(
            summary = "Audit effective user permissions",
            description = "Deep hierarchical resolution: calculates total effective permissions by combining direct realm roles, " +
                    "client roles, group memberships, and recursively expanded composite sub-roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Effective permission audit report",
                    content = @Content(schema = @Schema(implementation = EffectiveUserPermissionsDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found in Keycloak",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/mappings/users/{userId}/effective")
    public ResponseEntity<EffectiveUserPermissionsDTO> getEffectiveUserPermissions(
            @Parameter(description = "Keycloak User UUID", example = "usr-doc-204")
            @PathVariable("userId") String userId) {
        log.info("REST: Auditing effective permissions for user '{}'", userId);
        return ResponseEntity.ok(rolePermissionService.getEffectiveUserPermissions(userId));
    }
}

