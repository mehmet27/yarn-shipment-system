package com.yarnshipment.shipment.domain.fabric;

import java.io.Serial;
import java.util.UUID;

/** Kumaştaki iplik cinsi sevkiyatta gönderilenler arasında yok (409, {@code FABRIC_YARN_NOT_IN_SHIPMENT}). */
public final class FabricYarnNotInShipmentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FabricYarnNotInShipmentException(UUID shipmentId, String yarnType) {
        super("Fabric yarn not in shipment shipmentId=" + shipmentId + " yarnType=" + yarnType);
    }
}
