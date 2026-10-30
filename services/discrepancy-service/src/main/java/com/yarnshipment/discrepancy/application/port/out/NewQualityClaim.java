package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Yazılacak kalite itirazı; kimlikleri ve uyuşmazlık numarasını adapter verir.
 *
 * @param target itiraz edilen teslim
 * @param request açıklama, çuvallar ve fotoğraflar
 * @param openedBy açan kullanıcı
 * @param openedAt açılış zamanı
 * @param dueAt uyuşmazlığın hedef kapanış zamanı
 */
public record NewQualityClaim(
        ClaimTarget target, ClaimRequest request, UUID openedBy, Instant openedAt, Instant dueAt) {

    public NewQualityClaim {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(openedBy, "openedBy");
        Objects.requireNonNull(openedAt, "openedAt");
        Objects.requireNonNull(dueAt, "dueAt");
    }
}
