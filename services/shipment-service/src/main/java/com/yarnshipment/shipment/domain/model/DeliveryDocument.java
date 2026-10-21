package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İrsaliye üzerindeki değerler, fasoncunun okuduğu haliyle.
 *
 * @param shipmentNo irsaliyedeki sevkiyat numarası
 * @param date irsaliye tarihi
 * @param netKg irsaliyedeki net kilo
 * @param bagCount irsaliyedeki çuval sayısı
 * @param signedDocumentEvidenceId imzalı irsaliye fotoğrafı (evidence-service)
 */
public record DeliveryDocument(
        @Nullable String shipmentNo,
        @Nullable LocalDate date,
        @Nullable BigDecimal netKg,
        @Nullable Integer bagCount,
        @Nullable UUID signedDocumentEvidenceId) {

    /** İrsaliye bilgisi girilmedi. */
    public static final DeliveryDocument NONE = new DeliveryDocument(null, null, null, null, null);
}
