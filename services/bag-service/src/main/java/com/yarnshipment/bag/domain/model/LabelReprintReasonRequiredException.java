package com.yarnshipment.bag.domain.model;

import java.io.Serial;

/** Etiketi daha önce basılmış çuval gerekçesiz yeniden basılamaz (422, {@code LABEL_REPRINT_REASON_REQUIRED}). */
public final class LabelReprintReasonRequiredException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LabelReprintReasonRequiredException(String uniqueKey) {
        super("Reprint reason required uniqueKey=" + uniqueKey);
    }
}
