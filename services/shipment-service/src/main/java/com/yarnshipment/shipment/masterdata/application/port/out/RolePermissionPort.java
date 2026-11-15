package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Rol izinleri okuma ve yazma portu. Yazma, değişikliği yapan kullanıcıyı transaction'a işler (audit). */
public interface RolePermissionPort {

    /** Düzenlenebilir rollerin kayıtlı izinleri; kaydı olmayan rol boş kümedir. */
    Map<Role, Set<Permission>> findAll();

    /** Rolün izin kümesini verilene eşitler (eksikleri ekler, fazlaları siler). */
    void replace(UUID actorId, Role role, Set<Permission> permissions);
}
