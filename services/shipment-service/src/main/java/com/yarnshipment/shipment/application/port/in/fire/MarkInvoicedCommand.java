package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.common.security.Actor;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * "Fatura edildi" işareti.
 *
 * @param actor işaretleyen müdür (SEC-04)
 * @param shipmentId sevkiyat
 * @param invoiceNo ERP fatura numarası
 * @param invoicedOn fatura tarihi
 * @param note isteğe bağlı not
 */
public record MarkInvoicedCommand(
        Actor actor,
        UUID shipmentId,
        String invoiceNo,
        LocalDate invoicedOn,
        @Nullable String note) {

    public MarkInvoicedCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(invoiceNo, "invoiceNo");
        Objects.requireNonNull(invoicedOn, "invoicedOn");
    }
}
