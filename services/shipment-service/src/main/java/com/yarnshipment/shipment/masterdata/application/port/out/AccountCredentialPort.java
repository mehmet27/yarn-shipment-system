package com.yarnshipment.shipment.masterdata.application.port.out;

import java.util.UUID;

/** Şifre özeti, hesap durumu ve oturum işlemleri. Şifrenin kendisi hiçbir yerde loglanmaz ya da saklanmaz. */
public interface AccountCredentialPort {

    /**
     * Geçici şifre özetini yazar: kişi bir sonraki girişte değiştirmek zorundadır; kilidi kalkar, açık oturumlar
     * kapanır. Kullanıcı yoksa not-found fırlatır.
     */
    void setTemporaryPasswordHash(UUID userId, String passwordHash);

    /** Hesabı girişe açar. Kullanıcı yoksa not-found fırlatır. */
    void enable(UUID userId);

    /** Hesabı kapatır ve açık oturumlarını sonlandırır. Kullanıcı yoksa not-found fırlatır. */
    void disable(UUID userId);
}
