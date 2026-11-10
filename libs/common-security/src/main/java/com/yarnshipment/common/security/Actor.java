package com.yarnshipment.common.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * İsteği yapan kullanıcının token'dan çözülmüş hali (D-4). İşlemi yapan kişi her zaman buradan alınır, istek
 * gövdesinden değil (SEC-04).
 *
 * @param userId Keycloak {@code sub}
 * @param username {@code preferred_username}; kişisel veridir, loglanmaz
 * @param roles MFA kuralı uygulandıktan sonra geçerli roller
 * @param permissions rollerin {@link RolePermissionMatrix} ile açılmış izinleri
 * @param siteIds kullanıcının yetkili olduğu lokasyonlar ({@code site_ids} claim, GEN-07)
 * @param mfaVerified oturum iki adımlı doğrulamayla açıldı mı
 */
public record Actor(
        UUID userId,
        String username,
        Set<Role> roles,
        Set<Permission> permissions,
        Set<UUID> siteIds,
        boolean mfaVerified)
        implements Serializable {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(username, "username");
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
        siteIds = Set.copyOf(siteIds);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(Role... candidates) {
        return Arrays.stream(candidates).anyMatch(roles::contains);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    /** SEC-13: kullanıcı adı kişisel veridir. */
    @Override
    public String toString() {
        return "Actor[userId=" + userId + ", username=***, roles=" + roles + ", siteIds=" + siteIds + ", mfaVerified="
                + mfaVerified + "]";
    }
}
