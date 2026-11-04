package com.yarnshipment.shipment.masterdata.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Kimlik sağlayıcıda (Keycloak) şifre, durum ve oturum işlemleri. Şifre hiçbir yerde loglanmaz. */
public interface AccountCredentialPort {

    Optional<ResetTarget> findByUsername(String username);

    Optional<ResetTarget> findById(UUID userId);

    /**
     * Kalıcı şifreyi belirler, "şifre değiştir" zorunluluğunu kaldırır ve açık oturumları kapatır. Sağlayıcı şifreyi
     * kurala uymadığı için reddederse {@code WeakPasswordException}; ulaşılamazsa
     * {@code LoginAccountUnavailableException} fırlatır.
     */
    void setPassword(UUID userId, String password);

    /** Hesabı girişe açar. Kullanıcı yoksa not-found fırlatır. */
    void enable(UUID userId);

    /** Hesabı kapatır ve açık oturumlarını sonlandırır. Kullanıcı yoksa not-found fırlatır. */
    void disable(UUID userId);
}
