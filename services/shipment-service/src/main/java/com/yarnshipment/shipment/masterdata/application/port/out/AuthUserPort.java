package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Giriş denetimi için hesabı okur ve deneme sayaçlarını yazar. */
public interface AuthUserPort {

    /** Giriş adına göre hesap; büyük/küçük harf duyarsızdır. */
    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findById(UUID userId);

    /**
     * Başarısız denemeyi sayar; sayaç {@code maxAttempts}'e ulaşınca hesabı {@code lockFor} süre kilitler ve sayacı
     * sıfırlar. Sayım tek SQL ifadesiyle yapılır; eşzamanlı denemeler birbirini ezmez.
     */
    void recordFailedAttempt(UUID userId, int maxAttempts, Duration lockFor, Instant now);

    /** Başarılı girişi yazar: sayaç ve kilit sıfırlanır. */
    void recordSuccessfulLogin(UUID userId, Instant now);

    /** Kişinin kendi seçtiği şifre özetini yazar; zorunlu şifre değiştirme bayrağı kalkar. */
    void replacePasswordHash(UUID userId, String passwordHash, Instant now);
}
