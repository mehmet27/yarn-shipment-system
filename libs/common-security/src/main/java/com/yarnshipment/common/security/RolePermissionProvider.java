package com.yarnshipment.common.security;

import java.util.Collection;
import java.util.Set;

/**
 * Rollerin geçerli izinlerini veren kaynak. Varsayılan kaynak koddaki {@link RolePermissionMatrix}'tir; uygulama
 * bunun yerine yöneticinin Yetkiler ekranından düzenlediği kayıtları veren bir bean tanımlayabilir.
 */
@FunctionalInterface
public interface RolePermissionProvider {

    /** Koddaki varsayılan matris. */
    RolePermissionProvider DEFAULTS = RolePermissionMatrix::permissionsOf;

    /** Rollerin izinlerinin birleşimi. */
    Set<Permission> permissionsOf(Collection<Role> roles);
}
