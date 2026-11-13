package com.yarnshipment.shipment.masterdata.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Giriş denetimi için okunan hesap. Şifre özeti yalnızca doğrulamada kullanılır; hiçbir yanıtta ya da logda yer almaz.
 *
 * @param id kullanıcı kimliği
 * @param username giriş adı
 * @param fullName ad soyad
 * @param role tek rol
 * @param passwordHash şifre özeti
 * @param enabled hesap girişe açık mı
 * @param passwordChangeRequired kişi geçici şifreyle açıldı, kendi şifresini seçmeli
 * @param lockedUntil art arda yanlış deneme yüzünden hesabın kilitli olduğu an; kilit yoksa boş
 * @param siteIds kullanıcının göreceği lokasyonlar
 */
public record AuthUser(
        UUID id,
        String username,
        String fullName,
        StaffRole role,
        String passwordHash,
        boolean enabled,
        boolean passwordChangeRequired,
        @Nullable Instant lockedUntil,
        Set<UUID> siteIds) {

    public AuthUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(passwordHash, "passwordHash");
        siteIds = Set.copyOf(siteIds);
    }

    /** Kilit hâlâ sürüyor mu. */
    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    @Override
    public String toString() {
        return "AuthUser[id=" + id + ", role=" + role + ", enabled=" + enabled + "]";
    }
}
