package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.common.security.RolePermissionProvider;
import com.yarnshipment.shipment.masterdata.application.port.out.RolePermissionPort;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Rol izinlerini Yetkiler ekranının kayıtlarından verir (kısa süreli önbellekle). Yönetici ve sistem rolünün
 * izinleri kodda sabittir, kayıttan okunmaz.
 */
@Component
final class StoredRolePermissionProvider implements RolePermissionProvider {

    private final RolePermissionPort rolePermissions;
    private final RolePermissionCache cache;

    StoredRolePermissionProvider(RolePermissionPort rolePermissions, RolePermissionCache cache) {
        this.rolePermissions = rolePermissions;
        this.cache = cache;
    }

    @Override
    public Set<Permission> permissionsOf(Collection<Role> roles) {
        Map<Role, Set<Permission>> stored = cache.get(rolePermissions::findAll);
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        for (Role role : roles) {
            result.addAll(
                    RolePermissionMatrix.EDITABLE_ROLES.contains(role)
                            ? stored.getOrDefault(role, Set.of())
                            : RolePermissionMatrix.defaultsOf(role));
        }
        return Set.copyOf(result);
    }
}
