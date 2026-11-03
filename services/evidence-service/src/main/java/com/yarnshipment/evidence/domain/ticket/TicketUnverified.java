package com.yarnshipment.evidence.domain.ticket;

import com.yarnshipment.evidence.domain.model.EvidenceType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kilosu okunamayan fişle gerekçe seçilerek devam edildi (tasarım K0 "doğrulanamadı"). Son denemenin okumasına
 * bağlıdır; kayıt değişmez ve her okuma için en fazla bir tanedir.
 *
 * @param evidenceId son denemenin fotoğrafı
 * @param shipmentId sevkiyat
 * @param evidenceType {@code DISPATCH_SCALE} veya {@code DELIVERY_SCALE}
 * @param reason seçilen gerekçe
 * @param declaredAt gerekçenin seçildiği zaman
 */
public record TicketUnverified(
        UUID evidenceId, UUID shipmentId, EvidenceType evidenceType, UnverifiedReason reason, Instant declaredAt) {

    public TicketUnverified {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(evidenceType, "evidenceType");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(declaredAt, "declaredAt");
    }

    /** Yalnızca okunamamış fiş için gerekçe yazılabilir; güvenle okunmuş fişin karşılaştırması zaten yapılır. */
    public static TicketUnverified declare(TicketReading reading, UnverifiedReason reason, Instant at) {
        if (reading.status() != TicketReadingStatus.UNREADABLE) {
            throw new TicketNotUnreadableException(reading.evidenceId());
        }
        return new TicketUnverified(reading.evidenceId(), reading.shipmentId(), reading.evidenceType(), reason, at);
    }
}
