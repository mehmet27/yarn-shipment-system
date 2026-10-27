package com.yarnshipment.discrepancy.domain.ticket;

import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyatın kantar fişi (fiş no ve kantar adı) başka bir sevkiyatta da okunmuş.
 *
 * @param side fişin tarafı
 * @param ticketNo fiş numarası
 * @param otherShipmentId fişin daha önce görüldüğü sevkiyat
 */
public record TicketReuse(TicketSide side, String ticketNo, UUID otherShipmentId) {

    public TicketReuse {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(ticketNo, "ticketNo");
        Objects.requireNonNull(otherShipmentId, "otherShipmentId");
    }
}
