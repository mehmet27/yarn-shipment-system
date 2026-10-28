package com.yarnshipment.shipment.domain.evidence;

import java.util.UUID;

/**
 * evidence-service'teki bir kanıtın bu serviste gereken özeti.
 *
 * @param id kanıt kimliği
 * @param shipmentId kanıtın ait olduğu sevkiyat
 * @param type kanıt türü, ör. {@code SIGNED_DELIVERY_DOC}
 */
public record EvidenceReference(UUID id, UUID shipmentId, String type) {

    public static final String SIGNED_DELIVERY_DOC = "SIGNED_DELIVERY_DOC";

    /** Kanıt bu sevkiyatın imzalı irsaliyesi mi. */
    public boolean isSignedDocumentOf(UUID otherShipmentId) {
        return shipmentId.equals(otherShipmentId) && SIGNED_DELIVERY_DOC.equals(type);
    }
}
