package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Şifresi sıfırlanacak giriş hesabı.
 *
 * @param userId Keycloak kullanıcı kimliği
 * @param username giriş adı
 * @param enabled hesap girişe açık mı
 * @param phone SMS numarası; hesapta yoksa boş
 */
public record ResetTarget(
        UUID userId,
        String username,
        boolean enabled,
        @Nullable PhoneNumber phone) {

    public ResetTarget {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(username, "username");
    }
}
