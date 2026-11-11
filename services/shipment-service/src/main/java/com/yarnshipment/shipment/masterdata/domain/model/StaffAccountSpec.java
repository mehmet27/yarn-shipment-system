package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Açılacak fabrika içi hesap. İlk şifreyi yönetici belirler; kişi ilk girişte kendi şifresini seçmek zorundadır.
 *
 * @param username giriş adı
 * @param fullName ad soyad
 * @param role verilecek tek rol
 * @param password yöneticinin belirlediği geçici şifre; loglanmaz
 * @param siteIds kullanıcının göreceği lokasyonlar (etkin tüm lokasyonlar)
 */
public record StaffAccountSpec(String username, String fullName, StaffRole role, String password, List<UUID> siteIds) {

    public StaffAccountSpec {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(password, "password");
        siteIds = List.copyOf(siteIds);
    }

    @Override
    public String toString() {
        return "StaffAccountSpec[username=" + username + ", role=" + role + "]";
    }
}
