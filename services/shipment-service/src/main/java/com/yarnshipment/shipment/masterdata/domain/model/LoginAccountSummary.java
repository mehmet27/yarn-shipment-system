package com.yarnshipment.shipment.masterdata.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Kimlik sağlayıcıdaki (Keycloak) giriş hesabının listede gösterilen özeti. Şifre ya da kimlik bilgisi taşımaz.
 *
 * @param id Keycloak kullanıcı kimliği
 * @param username giriş adı
 * @param fullName ad soyad ya da unvan; profilde yoksa boş
 * @param roles uygulama rolleri, büyük harfle (ör. {@code ADMIN}); sıralı ve tekrarsız
 * @param enabled hesap girişe açık mı
 * @param passwordChangeRequired kişi geçici şifreyle açıldı, henüz kendi şifresini belirlemedi
 * @param createdAt hesabın açıldığı an; sağlayıcı vermezse boş
 */
public record LoginAccountSummary(
        String id,
        String username,
        @Nullable String fullName,
        List<String> roles,
        boolean enabled,
        boolean passwordChangeRequired,
        @Nullable Instant createdAt) {

    public LoginAccountSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        roles = roles.stream().distinct().sorted().toList();
    }

    /** Giriş adı ya da ad içinde büyük-küçük harf duyarsız arar. */
    public boolean matches(String text) {
        String needle = text.strip().toLowerCase(Locale.ROOT);
        return contains(username, needle) || contains(fullName, needle);
    }

    private static boolean contains(@Nullable String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
