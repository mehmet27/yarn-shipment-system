package com.yarnshipment.shipment.application.port.in.correction;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.correction.Correction;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin'in gerekçeli düzeltme isteği.
 *
 * @param actor düzeltmeyi yapan Admin
 * @param shipmentId sevkiyat
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04); düzeltme kaydının kimliği olur
 * @param request düzeltilecek kayıt, yeni değer ve gerekçe
 */
public record RecordCorrectionCommand(Actor actor, UUID shipmentId, UUID idempotencyKey, Correction.Request request) {

    public RecordCorrectionCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(request, "request");
    }
}
