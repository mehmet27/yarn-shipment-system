package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyatı tamamlama isteği: irsaliye müdüre ulaştı.
 *
 * @param actor tamamlayan kullanıcı
 * @param shipmentId sevkiyat
 * @param waybillNo irsaliye numarası
 * @param waybillEvidenceId evidence-service'e önceden yüklenen irsaliye belgesi
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 * @param acceptStockShortage merkez stok yetmiyorsa eksik kadar sistem girişi yazılmasının onayı
 */
public record CompleteShipmentCommand(
        Actor actor,
        UUID shipmentId,
        String waybillNo,
        UUID waybillEvidenceId,
        UUID idempotencyKey,
        boolean acceptStockShortage) {

    /** Stok eksiği onaylanmamış (varsayılan) istek. */
    public CompleteShipmentCommand(
            Actor actor, UUID shipmentId, String waybillNo, UUID waybillEvidenceId, UUID idempotencyKey) {
        this(actor, shipmentId, waybillNo, waybillEvidenceId, idempotencyKey, false);
    }

    public CompleteShipmentCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(waybillNo, "waybillNo");
        Objects.requireNonNull(waybillEvidenceId, "waybillEvidenceId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
