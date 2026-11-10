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
 */
public record CompleteShipmentCommand(
        Actor actor, UUID shipmentId, String waybillNo, UUID waybillEvidenceId, UUID idempotencyKey) {

    public CompleteShipmentCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(waybillNo, "waybillNo");
        Objects.requireNonNull(waybillEvidenceId, "waybillEvidenceId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
