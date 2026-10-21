package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.DeliveryMeasurement;
import java.util.Objects;
import java.util.UUID;

/**
 * Fasoncunun kör teslim girişi.
 *
 * @param actor gönderen fasoncu kullanıcısı (SEC-04)
 * @param shipmentId sevkiyat
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 * @param measurement girilen değerler
 */
public record SubmitDeliveryReceiptCommand(
        Actor actor, UUID shipmentId, UUID idempotencyKey, DeliveryMeasurement measurement) {

    public SubmitDeliveryReceiptCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(measurement, "measurement");
    }
}
