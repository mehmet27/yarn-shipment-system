package com.yarnshipment.audit.domain.model;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Zincir doğrulama işinin bir sonucu.
 *
 * @param id sonuç sırası
 * @param checkedAt doğrulamanın yapıldığı zaman
 * @param fromId doğrulanan ilk kayıt
 * @param toId doğrulanan son kayıt
 * @param ok zincir bozulmamışsa {@code true}
 * @param brokenAtId ilk bozuk kaydın sırası; bozulma yoksa boş
 */
public record ChainCheck(
        long id,
        Instant checkedAt,
        long fromId,
        long toId,
        boolean ok,
        @Nullable Long brokenAtId) {}
