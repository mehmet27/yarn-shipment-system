package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Açılacak giriş hesabı.
 *
 * @param username giriş adı
 * @param email e-posta (Keycloak kullanıcı profili zorunlu tutar)
 * @param displayName şoförün adı soyadı ya da fasoncunun unvanı
 * @param role Keycloak rolü
 * @param ownerId şoför ya da fasoncu kaydının kimliği
 * @param siteIds kullanıcının göreceği lokasyonlar (boş olamaz)
 * @param temporaryPassword ilk girişte değiştirilecek geçici şifre
 */
public record LoginAccountSpec(
        String username,
        String email,
        String displayName,
        LoginRole role,
        UUID ownerId,
        List<UUID> siteIds,
        String temporaryPassword) {

    public LoginAccountSpec {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(temporaryPassword, "temporaryPassword");
        siteIds = List.copyOf(siteIds);
    }
}
