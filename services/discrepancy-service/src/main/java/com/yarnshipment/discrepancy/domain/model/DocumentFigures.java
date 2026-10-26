package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fasoncunun imzalı irsaliyeden okuduğu değerler.
 *
 * @param shipmentNo irsaliyedeki sevkiyat numarası
 * @param netKg irsaliyedeki net kilo
 * @param bagCount irsaliyedeki çuval sayısı
 * @param signedDocumentEvidenceId imzalı belgenin kanıt kaydı; yüklenmediyse boş
 */
public record DocumentFigures(
        @Nullable String shipmentNo,
        @Nullable BigDecimal netKg,
        @Nullable Integer bagCount,
        @Nullable UUID signedDocumentEvidenceId) {

    /** İrsaliye bilgisi girilmedi. */
    public static final DocumentFigures NONE = new DocumentFigures(null, null, null, null);
}
