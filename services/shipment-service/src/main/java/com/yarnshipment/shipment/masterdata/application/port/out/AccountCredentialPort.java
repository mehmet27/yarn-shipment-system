package com.yarnshipment.shipment.masterdata.application.port.out;

import java.util.UUID;

/** Kimlik sağlayıcıda (Keycloak) şifre, durum ve oturum işlemleri. Şifre hiçbir yerde loglanmaz. */
public interface AccountCredentialPort {

    /**
     * Geçici şifre belirler: kişi bir sonraki girişte değiştirmek zorundadır; açık oturumlar kapanır. Sağlayıcı şifreyi
     * kurala uymadığı için reddederse {@code WeakPasswordException}; ulaşılamazsa
     * {@code LoginAccountUnavailableException} fırlatır.
     */
    void setTemporaryPassword(UUID userId, String password);

    /** Hesabı girişe açar. Kullanıcı yoksa not-found fırlatır. */
    void enable(UUID userId);

    /** Hesabı kapatır ve açık oturumlarını sonlandırır. Kullanıcı yoksa not-found fırlatır. */
    void disable(UUID userId);
}
