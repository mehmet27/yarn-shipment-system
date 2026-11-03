package com.yarnshipment.discrepancy.application.port.in.ticket;

import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.util.Objects;
import java.util.UUID;

/**
 * Kantar fişi üç denemede okunamadı, kullanıcı gerekçe seçip devam etti ({@code TicketUnverified}).
 *
 * @param eventId tetikleyen olayın kimliği; idempotency anahtarı (EVT-05)
 * @param eventType tetikleyen olayın tipi
 * @param evidenceId son denemenin fotoğrafı
 * @param shipmentId sevkiyat
 * @param side fişin tarafı
 * @param reason seçilen gerekçe
 */
public record OpenUnverifiedTicketCommand(
        UUID eventId, String eventType, UUID evidenceId, UUID shipmentId, TicketSide side, String reason) {

    public OpenUnverifiedTicketCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(reason, "reason");
    }
}
