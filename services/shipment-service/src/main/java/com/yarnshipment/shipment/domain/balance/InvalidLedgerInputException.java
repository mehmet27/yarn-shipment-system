package com.yarnshipment.shipment.domain.balance;

import java.io.Serial;

/** Bakiye, kumaş, iplik dönüşü veya fire isteğindeki alan geçersiz (400, {@code INVALID_LEDGER_INPUT}). */
public final class InvalidLedgerInputException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidLedgerInputException(String field) {
        super("Invalid ledger input field=" + field);
        this.field = field;
    }

    public InvalidLedgerInputException(String field, Throwable cause) {
        super("Invalid ledger input field=" + field, cause);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
