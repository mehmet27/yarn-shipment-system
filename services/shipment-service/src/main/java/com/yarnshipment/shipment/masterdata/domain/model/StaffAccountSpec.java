package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Açılacak fabrika içi hesap. Şifre içermez: kişi e-postayla gelen bağlantıdan kendi şifresini belirler.
 *
 * @param username giriş adı
 * @param email davet e-postasının gideceği adres
 * @param fullName ad soyad
 * @param role verilecek tek rol
 * @param siteIds kullanıcının göreceği lokasyonlar (boş olamaz)
 */
public record StaffAccountSpec(String username, String email, String fullName, StaffRole role, List<UUID> siteIds) {

    public StaffAccountSpec {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        siteIds = List.copyOf(siteIds);
    }
}
