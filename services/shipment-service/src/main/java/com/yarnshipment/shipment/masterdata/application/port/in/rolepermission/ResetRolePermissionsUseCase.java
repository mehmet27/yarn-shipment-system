package com.yarnshipment.shipment.masterdata.application.port.in.rolepermission;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;

/** Rolün izinlerini koddaki varsayılana döndürür. Yönetici rolü sabittir; yetki uçtadır (PERMISSION_MANAGE). */
public interface ResetRolePermissionsUseCase {

    void resetRolePermissions(Actor actor, Role role);
}
