package com.yarnshipment.shipment.domain.correction;

import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin'in gerekçeli düzeltmesi. Kayıt kilitlendikten sonra da yapılabilir; eski değer kaybolmaz, düzeltme kaydında ve
 * eski satırda kalır. Yalnızca Admin yapabildiği için kayıt aynı anda onaylanmış sayılır (karar #9).
 *
 * @param id düzeltme kaydı; istemcinin tekrar anahtarıdır, aynı istek ikinci kez uygulanmaz
 * @param shipmentId sevkiyat
 * @param target düzeltilen kayıt
 * @param oldValue düzeltmeden önceki değer
 * @param newValue yeni değer
 * @param reason gerekçe (kırpılmış, boş değil)
 * @param correctedAt sunucunun düzeltmeyi aldığı zaman
 */
public record Correction(
        UUID id,
        UUID shipmentId,
        CorrectionTarget target,
        CorrectedValue oldValue,
        CorrectedValue newValue,
        String reason,
        Instant correctedAt) {

    public static final int MAX_REASON_LENGTH = 500;

    public Correction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(oldValue, "oldValue");
        Objects.requireNonNull(newValue, "newValue");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(correctedAt, "correctedAt");
    }

    /**
     * İptal edilmemiş sevkiyatta girilmiş çıkış tartımı düzeltilir; yeni değer eskisinden farklı olmalıdır.
     *
     * @param request istenen düzeltme
     * @param id düzeltme kaydının kimliği
     * @param correctedAt sunucu zamanı
     */
    public static Correction of(Shipment shipment, Request request, UUID id, Instant correctedAt) {
        CorrectionTarget target = request.target();
        if (shipment.status() == ShipmentStatus.CANCELLED) {
            throw new InvalidCorrectionException("target");
        }
        CorrectedValue current =
                target.currentValue(shipment).orElseThrow(() -> new InvalidCorrectionException("target"));
        if (request.value().equals(current)) {
            throw new InvalidCorrectionException("value");
        }
        String reason = request.reason().strip();
        if (reason.isEmpty() || reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidCorrectionException("reason");
        }
        return new Correction(id, shipment.id(), target, current, request.value(), reason, correctedAt);
    }

    /**
     * İstenen düzeltme.
     *
     * @param target düzeltilecek kayıt
     * @param value yeni değer; türü kayda uymalıdır
     * @param reason gerekçe
     */
    public record Request(CorrectionTarget target, CorrectedValue value, String reason) {

        public Request {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
