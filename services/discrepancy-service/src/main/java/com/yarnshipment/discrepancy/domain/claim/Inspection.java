package com.yarnshipment.discrepancy.domain.claim;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir kalite itirazı için istenen numune ya da lab kontrolü. Sonuç bir kez girilir ve değişmez.
 *
 * @param id kimlik
 * @param shipmentId sevkiyat
 * @param discrepancyId itirazın uyuşmazlığı
 * @param kind numune ya da lab
 * @param request kim, ne zaman, hangi notla istedi
 * @param result sonuç; girilmemişse boş
 */
public record Inspection(
        UUID id,
        UUID shipmentId,
        UUID discrepancyId,
        InspectionKind kind,
        Request request,
        @Nullable Result result) {

    public Inspection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(discrepancyId, "discrepancyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(request, "request");
    }

    public boolean isPending() {
        return result == null;
    }

    /** Sonucu girer. Lab sonucu belgesiz olamaz; numune karşılaştırmasında belge isteğe bağlıdır. */
    public Inspection recordResult(Result recorded) {
        if (!isPending()) {
            throw new ClaimRejectedException(ClaimRejection.INSPECTION_RECORDED, "inspectionId=" + id);
        }
        if (kind == InspectionKind.LAB && recorded.evidenceId() == null) {
            throw new ClaimRejectedException(ClaimRejection.RESULT_EVIDENCE_REQUIRED, "inspectionId=" + id);
        }
        return new Inspection(id, shipmentId, discrepancyId, kind, request, recorded);
    }

    /**
     * Kontrol isteği.
     *
     * @param note isteğin notu
     * @param requestedBy isteyen
     * @param requestedAt istek zamanı
     */
    public record Request(@Nullable String note, UUID requestedBy, Instant requestedAt) {

        public Request {
            Objects.requireNonNull(requestedBy, "requestedBy");
            Objects.requireNonNull(requestedAt, "requestedAt");
        }
    }

    /**
     * Kontrol sonucu.
     *
     * @param summary sonucun özeti
     * @param evidenceId sonuç belgesi ({@code LAB_RESULT} kanıtı)
     * @param recordedBy sonucu giren
     * @param recordedAt sonucun girildiği zaman
     */
    public record Result(String summary, @Nullable UUID evidenceId, UUID recordedBy, Instant recordedAt) {

        public Result {
            Objects.requireNonNull(summary, "summary");
            Objects.requireNonNull(recordedBy, "recordedBy");
            Objects.requireNonNull(recordedAt, "recordedAt");
        }
    }
}
