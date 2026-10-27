package com.yarnshipment.evidence.domain.model;

/** shipment-service'e ulaşılamadı ya da beklenmeyen yanıt verdi; sevkiyat doğrulanamadan kanıt kabul edilmez. */
public final class ShipmentServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ShipmentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShipmentServiceUnavailableException(String message) {
        super(message);
    }
}
