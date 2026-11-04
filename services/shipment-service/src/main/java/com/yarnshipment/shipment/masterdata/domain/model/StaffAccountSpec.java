package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Açılacak fabrika içi hesap. Şifre içermez: kişi SMS'le gelen kodla kendi şifresini belirler.
 *
 * @param username giriş adı
 * @param email isteğe bağlı e-posta
 * @param phone SMS ile şifre kodunun gideceği telefon
 * @param fullName ad soyad
 * @param role verilecek tek rol
 * @param siteIds kullanıcının göreceği lokasyonlar (boş olamaz)
 */
public record StaffAccountSpec(
        String username,
        @Nullable String email,
        PhoneNumber phone,
        String fullName,
        StaffRole role,
        List<UUID> siteIds) {

    public StaffAccountSpec {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        siteIds = List.copyOf(siteIds);
    }
}
