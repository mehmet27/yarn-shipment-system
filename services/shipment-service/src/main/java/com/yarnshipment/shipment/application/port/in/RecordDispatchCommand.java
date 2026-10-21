package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import java.util.Objects;
import java.util.UUID;

/**
 * Depo çalışanının çıkış tartımı ve mühür girişi.
 *
 * @param actor giriş yapan depo çalışanı (SEC-04)
 * @param shipmentId sevkiyat
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 * @param measurement girilen değerler
 */
public record RecordDispatchCommand(
        Actor actor, UUID shipmentId, UUID idempotencyKey, DispatchMeasurement measurement) {

    public RecordDispatchCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(measurement, "measurement");
    }
}
