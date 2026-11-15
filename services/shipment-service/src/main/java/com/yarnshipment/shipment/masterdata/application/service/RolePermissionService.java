package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.ListRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.ResetRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.SaveRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.RolePermissionPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rol izinlerini yönetir. Yönetici rolü ve yalnızca yöneticide olan izinler (kullanıcı, ayar ve yetki yönetimi)
 * sabittir: yönetici kendi yetkisini ve sistem yönetimini bu yoldan değiştiremez, başka role de veremez.
 */
@Service
public class RolePermissionService
        implements ListRolePermissionsUseCase, SaveRolePermissionsUseCase, ResetRolePermissionsUseCase {

    private final RolePermissionPort rolePermissions;

    public RolePermissionService(RolePermissionPort rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Role, Set<Permission>> listRolePermissions() {
        Map<Role, Set<Permission>> stored = rolePermissions.findAll();
        Map<Role, Set<Permission>> result = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            if (role == Role.SYSTEM) {
                continue;
            }
            result.put(
                    role,
                    RolePermissionMatrix.EDITABLE_ROLES.contains(role)
                            ? stored.getOrDefault(role, Set.of())
                            : RolePermissionMatrix.defaultsOf(role));
        }
        return result;
    }

    @Override
    @Transactional
    public void saveRolePermissions(Actor actor, Role role, Set<Permission> permissions) {
        requireEditable(role);
        Set<Permission> requested = permissions.isEmpty() ? Set.of() : EnumSet.copyOf(permissions);
        for (Permission permission : requested) {
            if (RolePermissionMatrix.ADMIN_ONLY.contains(permission)) {
                throw new InvalidMasterDataException("permissions", "admin only: " + permission);
            }
        }
        rolePermissions.replace(actor.userId(), role, requested);
    }

    @Override
    @Transactional
    public void resetRolePermissions(Actor actor, Role role) {
        requireEditable(role);
        rolePermissions.replace(actor.userId(), role, RolePermissionMatrix.defaultsOf(role));
    }

    private static void requireEditable(Role role) {
        if (!RolePermissionMatrix.EDITABLE_ROLES.contains(role)) {
            throw new InvalidMasterDataException("role", "fixed role: " + role);
        }
    }
}
