package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Açılacak giriş hesabı.
 *
 * @param username giriş adı
 * @param email isteğe bağlı e-posta; şoför ve fasoncunun olmayabilir
 * @param phone SMS ile şifre kodunun gideceği telefon
 * @param displayName şoförün adı soyadı ya da fasoncunun unvanı
 * @param role Keycloak rolü
 * @param ownerId şoför ya da fasoncu kaydının kimliği
 * @param siteIds kullanıcının göreceği lokasyonlar (boş olamaz)
 */
public record LoginAccountSpec(
        String username,
        @Nullable String email,
        PhoneNumber phone,
        String displayName,
        LoginRole role,
        UUID ownerId,
        List<UUID> siteIds) {

    public LoginAccountSpec {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(ownerId, "ownerId");
        siteIds = List.copyOf(siteIds);
    }
}
