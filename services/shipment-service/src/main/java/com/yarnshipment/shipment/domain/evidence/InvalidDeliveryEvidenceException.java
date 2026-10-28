package com.yarnshipment.shipment.domain.evidence;

import java.io.Serial;
import java.util.UUID;

/** Teslim girişindeki imzalı irsaliye kanıtı yok, başka sevkiyata ait ya da imzalı irsaliye değil. */
public final class InvalidDeliveryEvidenceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidDeliveryEvidenceException(UUID shipmentId, UUID evidenceId) {
        super("Invalid signed document evidence shipmentId=" + shipmentId + " evidenceId=" + evidenceId);
    }
}
