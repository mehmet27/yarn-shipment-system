package com.yarnshipment.shipment.domain.completion;

import java.io.Serial;

/** Tamamlama isteği geçersiz, ör. irsaliye numarası boş (400, {@code INVALID_COMPLETION}). */
public final class InvalidCompletionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidCompletionException(String field) {
        super("Invalid completion field=" + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
