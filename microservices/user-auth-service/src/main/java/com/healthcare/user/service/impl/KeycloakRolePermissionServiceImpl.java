package com.healthcare.user.service.impl;

import com.healthcare.user.config.KeycloakAdminConfigProperties;
import com.healthcare.user.dto.*;
import com.healthcare.user.exception.KeycloakOperationException;
import com.healthcare.user.exception.KeycloakResourceConflictException;
import com.healthcare.user.exception.KeycloakResourceNotFoundException;
import com.healthcare.user.service.KeycloakRolePermissionService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Production-ready implementation of Keycloak Role and Permission Management using Keycloak Admin Client SDK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakRolePermissionServiceImpl implements KeycloakRolePermissionService {

    private final Keycloak keycloak;
    private final KeycloakAdminConfigProperties properties;

    private RealmResource getRealm() {
        return keycloak.realm(properties.getRealm());
    }

    private String getClientInternalId(String clientId) {
        List<ClientRepresentation> clients = getRealm().clients().findByClientId(clientId);
        if (clients == null || clients.isEmpty()) {
            throw new KeycloakResourceNotFoundException("Keycloak Client not found with clientId: " + clientId);
        }
        return clients.get(0).getId();
    }

    private RolesResource getRolesResource(boolean isClientRole, String clientId) {
        if (isClientRole) {
            if (clientId == null || clientId.isBlank()) {
                throw new IllegalArgumentException("Client ID must be specified for client-level role operations.");
            }
            String clientInternalId = getClientInternalId(clientId);
            return getRealm().clients().get(clientInternalId).roles();
        }
        return getRealm().roles();
    }

    @Override
    public RoleResponseDTO createRole(RoleRequestDTO request) {
        log.info("Creating role '{}' in Keycloak (ClientRole: {}, ClientId: {})",
                request.getName(), request.isClientRole(), request.getClientId());

        RolesResource rolesResource = getRolesResource(request.isClientRole(), request.getClientId());

        // Check if role already exists to ensure idempotency
        try {
            RoleRepresentation existing = rolesResource.get(request.getName()).toRepresentation();
            if (existing != null) {
                throw new KeycloakResourceConflictException("Role '" + request.getName() + "' already exists in realm " + properties.getRealm());
            }
        } catch (NotFoundException ignored) {
            // Expected if role does not exist
        }

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(request.getName());
        roleRep.setDescription(request.getDescription());
        roleRep.setComposite(request.isComposite());
        roleRep.setClientRole(request.isClientRole());

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            roleRep.setAttributes(request.getAttributes());
        }

        try {
            rolesResource.create(roleRep);
            log.info("Role '{}' created successfully.", request.getName());

            // Handle composite sub-roles if provided
            if (request.isComposite() && request.getSubRoleNames() != null && !request.getSubRoleNames().isEmpty()) {
                addSubRolesToComposite(request.getName(), request.getSubRoleNames(), request.isClientRole(), request.getClientId());
            }

            return getRoleByName(request.getName(), request.isClientRole(), request.getClientId());
        } catch (WebApplicationException ex) {
            if (ex.getResponse() != null && ex.getResponse().getStatus() == 409) {
                throw new KeycloakResourceConflictException("Role '" + request.getName() + "' already exists.");
            }
            throw new KeycloakOperationException("Failed to create role in Keycloak: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<RoleResponseDTO> getAllRealmRoles() {
        log.info("Fetching all realm-level roles from Keycloak realm '{}'", properties.getRealm());
        List<RoleRepresentation> roles = getRealm().roles().list();
        return roles.stream().map(this::toRoleResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<RoleResponseDTO> getAllClientRoles(String clientId) {
        log.info("Fetching all client-level roles for client '{}'", clientId);
        String clientInternalId = getClientInternalId(clientId);
        List<RoleRepresentation> roles = getRealm().clients().get(clientInternalId).roles().list();
        return roles.stream().map(this::toRoleResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<RoleResponseDTO> searchRoles(String query, boolean clientRole, String clientId) {
        log.info("Searching roles with prefix/query: '{}', clientRole: {}", query, clientRole);
        RolesResource rolesResource = getRolesResource(clientRole, clientId);
        List<RoleRepresentation> roles;
        if (query != null && !query.isBlank()) {
            roles = rolesResource.list(query, false);
        } else {
            roles = rolesResource.list();
        }
        return roles.stream().map(this::toRoleResponseDTO).collect(Collectors.toList());
    }

    @Override
    public RoleResponseDTO getRoleByName(String roleName, boolean clientRole, String clientId) {
        log.info("Fetching role details for '{}' (clientRole: {})", roleName, clientRole);
        RolesResource rolesResource = getRolesResource(clientRole, clientId);
        try {
            RoleResource roleResource = rolesResource.get(roleName);
            RoleRepresentation rep = roleResource.toRepresentation();

            RoleResponseDTO dto = toRoleResponseDTO(rep);
            if (rep.isComposite()) {
                Set<RoleRepresentation> compositeRoles = roleResource.getRoleComposites();
                if (compositeRoles != null && !compositeRoles.isEmpty()) {
                    dto.setCompositeSubRoles(compositeRoles.stream().map(this::toRoleResponseDTO).collect(Collectors.toList()));
                }
            }
            return dto;
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("Role '" + roleName + "' not found in Keycloak.");
        }
    }

    @Override
    public RoleResponseDTO updateRole(String roleName, RoleRequestDTO request) {
        log.info("Updating role '{}' in Keycloak", roleName);
        RolesResource rolesResource = getRolesResource(request.isClientRole(), request.getClientId());
        try {
            RoleResource roleResource = rolesResource.get(roleName);
            RoleRepresentation rep = roleResource.toRepresentation();

            if (request.getName() != null && !request.getName().isBlank()) {
                rep.setName(request.getName());
            }
            if (request.getDescription() != null) {
                rep.setDescription(request.getDescription());
            }
            if (request.getAttributes() != null) {
                rep.setAttributes(request.getAttributes());
            }

            roleResource.update(rep);
            log.info("Role '{}' updated successfully.", roleName);

            if (request.getSubRoleNames() != null) {
                addSubRolesToComposite(rep.getName(), request.getSubRoleNames(), request.isClientRole(), request.getClientId());
            }

            return getRoleByName(rep.getName(), request.isClientRole(), request.getClientId());
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("Role '" + roleName + "' not found for update.");
        }
    }

    @Override
    public void deleteRole(String roleName, boolean clientRole, String clientId) {
        log.info("Deleting role '{}' (clientRole: {})", roleName, clientRole);
        RolesResource rolesResource = getRolesResource(clientRole, clientId);
        try {
            rolesResource.deleteRole(roleName);
            log.info("Role '{}' deleted successfully.", roleName);
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("Role '" + roleName + "' not found for deletion.");
        } catch (WebApplicationException e) {
            throw new KeycloakOperationException("Failed to delete role '" + roleName + "': " + e.getMessage(), e);
        }
    }

    @Override
    public RoleResponseDTO addSubRolesToComposite(String parentRoleName, List<String> subRoleNames, boolean clientRole, String clientId) {
        log.info("Adding sub-roles {} to composite parent role '{}'", subRoleNames, parentRoleName);
        RolesResource rolesResource = getRolesResource(clientRole, clientId);
        try {
            RoleResource parentRole = rolesResource.get(parentRoleName);
            List<RoleRepresentation> subRolesToAdd = new ArrayList<>();

            for (String subName : subRoleNames) {
                try {
                    RoleRepresentation subRep = getRealm().roles().get(subName).toRepresentation();
                    subRolesToAdd.add(subRep);
                } catch (NotFoundException e) {
                    throw new KeycloakResourceNotFoundException("Sub-role '" + subName + "' does not exist in realm.");
                }
            }

            parentRole.addComposites(subRolesToAdd);
            log.info("Sub-roles added to '{}' composite hierarchy.", parentRoleName);
            return getRoleByName(parentRoleName, clientRole, clientId);
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("Parent composite role '" + parentRoleName + "' not found.");
        }
    }

    @Override
    public RoleResponseDTO removeSubRolesFromComposite(String parentRoleName, List<String> subRoleNames, boolean clientRole, String clientId) {
        log.info("Removing sub-roles {} from composite parent role '{}'", subRoleNames, parentRoleName);
        RolesResource rolesResource = getRolesResource(clientRole, clientId);
        try {
            RoleResource parentRole = rolesResource.get(parentRoleName);
            List<RoleRepresentation> subRolesToRemove = new ArrayList<>();

            for (String subName : subRoleNames) {
                try {
                    RoleRepresentation subRep = getRealm().roles().get(subName).toRepresentation();
                    subRolesToRemove.add(subRep);
                } catch (NotFoundException ignored) {}
            }

            if (!subRolesToRemove.isEmpty()) {
                parentRole.deleteComposites(subRolesToRemove);
            }
            return getRoleByName(parentRoleName, clientRole, clientId);
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("Parent composite role '" + parentRoleName + "' not found.");
        }
    }

    @Override
    public void assignRolesToUser(UserRoleMappingRequestDTO request) {
        log.info("Assigning roles {} to user '{}'", request.getRoleNames(), request.getUserId());
        UserResource userResource = getRealm().users().get(request.getUserId());
        validateUserExists(userResource, request.getUserId());

        if (request.isClientRole()) {
            String clientInternalId = getClientInternalId(request.getClientId());
            RoleScopeResource clientRoleScope = userResource.roles().clientLevel(clientInternalId);
            RolesResource clientRolesResource = getRealm().clients().get(clientInternalId).roles();

            List<RoleRepresentation> rolesToAdd = fetchRoles(clientRolesResource, request.getRoleNames());
            clientRoleScope.add(rolesToAdd);
        } else {
            RoleScopeResource realmRoleScope = userResource.roles().realmLevel();
            RolesResource realmRolesResource = getRealm().roles();

            List<RoleRepresentation> rolesToAdd = fetchRoles(realmRolesResource, request.getRoleNames());
            realmRoleScope.add(rolesToAdd);
        }
        log.info("Roles assigned successfully to user '{}'", request.getUserId());
    }

    @Override
    public void revokeRolesFromUser(UserRoleMappingRequestDTO request) {
        log.info("Revoking roles {} from user '{}'", request.getRoleNames(), request.getUserId());
        UserResource userResource = getRealm().users().get(request.getUserId());
        validateUserExists(userResource, request.getUserId());

        if (request.isClientRole()) {
            String clientInternalId = getClientInternalId(request.getClientId());
            RoleScopeResource clientRoleScope = userResource.roles().clientLevel(clientInternalId);
            RolesResource clientRolesResource = getRealm().clients().get(clientInternalId).roles();

            List<RoleRepresentation> rolesToRemove = fetchRoles(clientRolesResource, request.getRoleNames());
            clientRoleScope.remove(rolesToRemove);
        } else {
            RoleScopeResource realmRoleScope = userResource.roles().realmLevel();
            RolesResource realmRolesResource = getRealm().roles();

            List<RoleRepresentation> rolesToRemove = fetchRoles(realmRolesResource, request.getRoleNames());
            realmRoleScope.remove(rolesToRemove);
        }
        log.info("Roles revoked successfully from user '{}'", request.getUserId());
    }

    @Override
    public void assignRolesToGroup(GroupRoleMappingRequestDTO request) {
        log.info("Assigning roles {} to group '{}'", request.getRoleNames(), request.getGroupId());
        GroupResource groupResource = getRealm().groups().group(request.getGroupId());

        if (request.isClientRole()) {
            String clientInternalId = getClientInternalId(request.getClientId());
            RoleScopeResource clientRoleScope = groupResource.roles().clientLevel(clientInternalId);
            RolesResource clientRolesResource = getRealm().clients().get(clientInternalId).roles();

            List<RoleRepresentation> rolesToAdd = fetchRoles(clientRolesResource, request.getRoleNames());
            clientRoleScope.add(rolesToAdd);
        } else {
            RoleScopeResource realmRoleScope = groupResource.roles().realmLevel();
            RolesResource realmRolesResource = getRealm().roles();

            List<RoleRepresentation> rolesToAdd = fetchRoles(realmRolesResource, request.getRoleNames());
            realmRoleScope.add(rolesToAdd);
        }
        log.info("Roles assigned successfully to group '{}'", request.getGroupId());
    }

    @Override
    public void revokeRolesFromGroup(GroupRoleMappingRequestDTO request) {
        log.info("Revoking roles {} from group '{}'", request.getRoleNames(), request.getGroupId());
        GroupResource groupResource = getRealm().groups().group(request.getGroupId());

        if (request.isClientRole()) {
            String clientInternalId = getClientInternalId(request.getClientId());
            RoleScopeResource clientRoleScope = groupResource.roles().clientLevel(clientInternalId);
            RolesResource clientRolesResource = getRealm().clients().get(clientInternalId).roles();

            List<RoleRepresentation> rolesToRemove = fetchRoles(clientRolesResource, request.getRoleNames());
            clientRoleScope.remove(rolesToRemove);
        } else {
            RoleScopeResource realmRoleScope = groupResource.roles().realmLevel();
            RolesResource realmRolesResource = getRealm().roles();

            List<RoleRepresentation> rolesToRemove = fetchRoles(realmRolesResource, request.getRoleNames());
            realmRoleScope.remove(rolesToRemove);
        }
        log.info("Roles revoked successfully from group '{}'", request.getGroupId());
    }

    @Override
    public EffectiveUserPermissionsDTO getEffectiveUserPermissions(String userId) {
        log.info("Auditing effective permissions and composite roles for user '{}'", userId);
        UserResource userResource = getRealm().users().get(userId);
        UserRepresentation userRep;
        try {
            userRep = userResource.toRepresentation();
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("User not found with ID: " + userId);
        }

        RoleMappingResource roleMappingResource = userResource.roles();

        // 1. Direct and Effective Realm Roles
        List<String> directRealmRoles = roleMappingResource.realmLevel().listAll().stream()
                .map(RoleRepresentation::getName).collect(Collectors.toList());
        List<String> effectiveRealmRoles = roleMappingResource.realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName).collect(Collectors.toList());

        // 2. Direct and Effective Client Roles across all clients
        Map<String, List<String>> directClientRoles = new HashMap<>();
        Map<String, List<String>> effectiveClientRoles = new HashMap<>();

        List<ClientRepresentation> clients = getRealm().clients().findAll();
        for (ClientRepresentation client : clients) {
            RoleScopeResource clientRoleScope = roleMappingResource.clientLevel(client.getId());
            List<String> direct = clientRoleScope.listAll().stream()
                    .map(RoleRepresentation::getName).collect(Collectors.toList());
            List<String> effective = clientRoleScope.listEffective().stream()
                    .map(RoleRepresentation::getName).collect(Collectors.toList());

            if (!direct.isEmpty()) {
                directClientRoles.put(client.getClientId(), direct);
            }
            if (!effective.isEmpty()) {
                effectiveClientRoles.put(client.getClientId(), effective);
            }
        }

        int totalEffective = effectiveRealmRoles.size() + effectiveClientRoles.values().stream().mapToInt(List::size).sum();

        return EffectiveUserPermissionsDTO.builder()
                .userId(userRep.getId())
                .username(userRep.getUsername())
                .email(userRep.getEmail())
                .directRealmRoles(directRealmRoles)
                .compositeEffectiveRealmRoles(effectiveRealmRoles)
                .directClientRoles(directClientRoles)
                .compositeEffectiveClientRoles(effectiveClientRoles)
                .totalEffectiveRolesCount(totalEffective)
                .build();
    }

    private void validateUserExists(UserResource userResource, String userId) {
        try {
            userResource.toRepresentation();
        } catch (NotFoundException e) {
            throw new KeycloakResourceNotFoundException("User not found with ID: " + userId);
        }
    }

    private List<RoleRepresentation> fetchRoles(RolesResource rolesResource, List<String> roleNames) {
        List<RoleRepresentation> list = new ArrayList<>();
        for (String roleName : roleNames) {
            try {
                list.add(rolesResource.get(roleName).toRepresentation());
            } catch (NotFoundException e) {
                throw new KeycloakResourceNotFoundException("Role '" + roleName + "' does not exist in Keycloak.");
            }
        }
        return list;
    }

    private RoleResponseDTO toRoleResponseDTO(RoleRepresentation rep) {
        if (rep == null) return null;
        return RoleResponseDTO.builder()
                .id(rep.getId())
                .name(rep.getName())
                .description(rep.getDescription())
                .composite(rep.isComposite() != null && rep.isComposite())
                .clientRole(rep.getClientRole() != null && rep.getClientRole())
                .containerId(rep.getContainerId())
                .attributes(rep.getAttributes())
                .build();
    }
}
