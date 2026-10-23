package com.yarnshipment.shipment.masterdata.domain.model;

/**
 * Açılan giriş hesabı. Geçici şifre yalnızca bu yanıtta görünür; saklanmaz ve loglanmaz.
 *
 * @param username giriş adı
 * @param temporaryPassword ilk girişte değiştirilecek şifre
 */
public record LoginAccount(String username, String temporaryPassword) {

    @Override
    public String toString() {
        return "LoginAccount[username=" + username + "]";
    }
}
