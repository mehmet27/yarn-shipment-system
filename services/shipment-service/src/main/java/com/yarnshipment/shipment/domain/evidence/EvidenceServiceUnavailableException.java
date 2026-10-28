package com.yarnshipment.shipment.domain.evidence;

import java.io.Serial;

/** evidence-service'e ulaşılamadı ya da beklenmeyen yanıt verdi. */
public final class EvidenceServiceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EvidenceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvidenceServiceUnavailableException(String message) {
        super(message);
    }
}
