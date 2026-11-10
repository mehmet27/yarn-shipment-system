package com.yarnshipment.shipment.domain.evidence;

import java.io.Serial;
import java.util.UUID;

/** Tamamlamadaki irsaliye belgesi yok, başka sevkiyata ait ya da irsaliye türünde değil. */
public final class InvalidWaybillEvidenceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidWaybillEvidenceException(UUID shipmentId, UUID evidenceId) {
        super("Invalid waybill evidence shipmentId=" + shipmentId + " evidenceId=" + evidenceId);
    }
}
