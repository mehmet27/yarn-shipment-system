package com.yarnshipment.shipment.domain.correction;

import java.io.Serial;

/**
 * Düzeltme isteği geçersiz (400, {@code INVALID_CORRECTION}): değer tutarsız, gerekçe boş, değer değişmiyor
 * ({@code value}) ya da düzeltilecek kayıt yok ({@code target}).
 */
public final class InvalidCorrectionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidCorrectionException(String field) {
        super("Invalid correction field=" + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
