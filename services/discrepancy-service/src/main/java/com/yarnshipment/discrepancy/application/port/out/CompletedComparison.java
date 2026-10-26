package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.ComparisonResult;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kaydedilecek karşılaştırma.
 *
 * @param input iki tarafın değerleri
 * @param result sonuç ve açılacak uyuşmazlıklar
 * @param comparedAt karşılaştırma zamanı
 * @param dueAt açılan uyuşmazlıkların hedef kapanış zamanı
 * @param actorId kayıtların {@code created_by} değeri
 */
public record CompletedComparison(
        ComparisonInput input, ComparisonResult result, Instant comparedAt, Instant dueAt, UUID actorId) {

    public CompletedComparison {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(comparedAt, "comparedAt");
        Objects.requireNonNull(dueAt, "dueAt");
        Objects.requireNonNull(actorId, "actorId");
    }
}
