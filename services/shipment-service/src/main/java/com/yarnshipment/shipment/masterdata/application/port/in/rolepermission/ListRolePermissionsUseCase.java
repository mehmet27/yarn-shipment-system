package com.yarnshipment.shipment.masterdata.application.port.in.rolepermission;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import java.util.Map;
import java.util.Set;

/** Rollerin geçerli izinlerini listeler; yetki uçtadır (PERMISSION_MANAGE). */
public interface ListRolePermissionsUseCase {

    /** Her rol için geçerli izinler (yönetici ve sistem rolü koddan, diğerleri kayıttan). */
    Map<Role, Set<Permission>> listRolePermissions();
}
