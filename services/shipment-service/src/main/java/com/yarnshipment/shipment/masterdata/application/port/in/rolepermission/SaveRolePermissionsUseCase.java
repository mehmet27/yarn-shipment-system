package com.yarnshipment.shipment.masterdata.application.port.in.rolepermission;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import java.util.Set;

/**
 * Rolün izin kümesini kaydeder. Yönetici rolü ve yalnızca yöneticide olan izinler değiştirilemez
 * ({@code InvalidMasterDataException}). Yetki uçtadır (PERMISSION_MANAGE), iz audit_local'den düşer.
 */
public interface SaveRolePermissionsUseCase {

    void saveRolePermissions(Actor actor, Role role, Set<Permission> permissions);
}
