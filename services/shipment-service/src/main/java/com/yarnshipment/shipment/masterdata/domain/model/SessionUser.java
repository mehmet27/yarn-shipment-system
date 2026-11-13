package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Set;
import java.util.UUID;

/**
 * Giriş başarılı olunca oturuma yazılacak kullanıcı. Şifre bilgisi taşımaz.
 *
 * @param id kullanıcı kimliği
 * @param username giriş adı
 * @param fullName ad soyad
 * @param role tek rol
 * @param siteIds kullanıcının göreceği lokasyonlar
 * @param passwordChangeRequired kişi şifresini değiştirene kadar başka işlem yapamaz
 */
public record SessionUser(
        UUID id,
        String username,
        String fullName,
        StaffRole role,
        Set<UUID> siteIds,
        boolean passwordChangeRequired) {

    public SessionUser {
        siteIds = Set.copyOf(siteIds);
    }

    public static SessionUser of(AuthUser user) {
        return new SessionUser(
                user.id(),
                user.username(),
                user.fullName(),
                user.role(),
                user.siteIds(),
                user.passwordChangeRequired());
    }

    /** Şifresini değiştirmiş kişinin oturum kullanıcısı. */
    public SessionUser withPasswordChanged() {
        return new SessionUser(id, username, fullName, role, siteIds, false);
    }

    @Override
    public String toString() {
        return "SessionUser[id=" + id + ", role=" + role + "]";
    }
}
