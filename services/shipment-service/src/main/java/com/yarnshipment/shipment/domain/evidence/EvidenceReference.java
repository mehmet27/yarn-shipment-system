package com.yarnshipment.shipment.domain.evidence;

import java.util.UUID;

/**
 * evidence-service'teki bir kanıtın bu serviste gereken özeti.
 *
 * @param id kanıt kimliği
 * @param shipmentId kanıtın ait olduğu sevkiyat
 * @param type kanıt türü, ör. {@code WAYBILL}
 * @param status kanıt durumu, {@code VALID} ya da {@code INVALIDATED}
 */
public record EvidenceReference(UUID id, UUID shipmentId, String type, String status) {

    public static final String WAYBILL = "WAYBILL";
    public static final String VALID = "VALID";

    /** Kanıt bu sevkiyatın geçerli irsaliye belgesi mi. */
    public boolean isWaybillOf(UUID otherShipmentId) {
        return shipmentId.equals(otherShipmentId) && WAYBILL.equals(type) && VALID.equals(status);
    }
}
