package com.yarnshipment.discrepancy.domain.rule;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir kuralın geçerlilik aralığıyla saklanan bir sürümü. Kural değişince eski sürüm kapanır, yenisi eklenir; geçmiş
 * silinmez.
 *
 * @param value kuralın değeri, ör. {@link ToleranceBands}
 * @param validFrom geçerlilik başlangıcı
 * @param validTo geçerlilik sonu; güncel sürümde boş
 * @param setBy kuralı giren kullanıcı; ilk kuralda sistem hesabı
 * @param <T> kural değeri
 */
public record SettingVersion<T>(
        T value, Instant validFrom, @Nullable Instant validTo, UUID setBy) {

    public SettingVersion {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(validFrom, "validFrom");
        Objects.requireNonNull(setBy, "setBy");
    }
}
