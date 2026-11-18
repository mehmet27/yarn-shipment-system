package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.time.LocalDate;
import java.util.List;
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
 * @param lines iplik başına faturalanan kilo; boşsa kalan kilonun tamamı iplik oranıyla faturalanır
 */
public record MarkInvoicedCommand(
        Actor actor,
        UUID shipmentId,
        String invoiceNo,
        LocalDate invoicedOn,
        @Nullable String note,
        List<YarnShare> lines) {

    /** İplik satırı olmayan komut: kalanın tamamı faturalanır. */
    public MarkInvoicedCommand(
            Actor actor, UUID shipmentId, String invoiceNo, LocalDate invoicedOn, @Nullable String note) {
        this(actor, shipmentId, invoiceNo, invoicedOn, note, List.of());
    }

    public MarkInvoicedCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(invoiceNo, "invoiceNo");
        Objects.requireNonNull(invoicedOn, "invoicedOn");
        lines = List.copyOf(lines);
    }
}
