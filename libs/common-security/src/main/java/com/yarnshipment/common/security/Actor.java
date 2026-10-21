package com.yarnshipment.common.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İsteği yapan kullanıcının token'dan çözülmüş hali (D-4). İşlemi yapan kişi her zaman buradan alınır, istek
 * gövdesinden değil (SEC-04).
 *
 * @param userId Keycloak {@code sub}
 * @param username {@code preferred_username}; kişisel veridir, loglanmaz
 * @param roles MFA kuralı uygulandıktan sonra geçerli roller
 * @param permissions rollerin {@link RolePermissionMatrix} ile açılmış izinleri
 * @param subcontractorId yalnızca fasoncu kullanıcılarında dolu ({@code subcontractor_id} claim)
 * @param driverId yalnızca şoför kullanıcılarında dolu ({@code driver_id} claim)
 * @param siteIds kullanıcının yetkili olduğu lokasyonlar ({@code site_ids} claim, GEN-07)
 * @param mfaVerified oturum iki adımlı doğrulamayla açıldı mı
 */
public record Actor(
        UUID userId,
        String username,
        Set<Role> roles,
        Set<Permission> permissions,
        @Nullable UUID subcontractorId,
        @Nullable UUID driverId,
        Set<UUID> siteIds,
        boolean mfaVerified)
        implements Serializable {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(username, "username");
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
        siteIds = Set.copyOf(siteIds);
        requireScopeId(roles, Role.SUBCONTRACTOR, subcontractorId);
        requireScopeId(roles, Role.DRIVER, driverId);
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

    /** Dış rolün kapsam kimliği olmadan aktör oluşmaz; aksi halde kapsam yalnızca lokasyona düşerdi. */
    private static void requireScopeId(Set<Role> roles, Role role, @Nullable UUID scopeId) {
        if (roles.contains(role) && scopeId == null) {
            throw new IllegalArgumentException("Scope id is required for role=" + role);
        }
    }

    /** SEC-13: kullanıcı adı kişisel veridir. */
    @Override
    public String toString() {
        return "Actor[userId=" + userId + ", username=***, roles=" + roles + ", subcontractorId=" + subcontractorId
                + ", driverId=" + driverId + ", siteIds=" + siteIds + ", mfaVerified=" + mfaVerified + "]";
    }
}
