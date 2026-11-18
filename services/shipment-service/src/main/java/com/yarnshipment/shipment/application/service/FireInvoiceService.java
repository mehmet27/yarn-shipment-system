package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fire.CloseFireSettlementUseCase;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementQuery;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedCommand;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort.InvoiceRecord;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.application.service.invoice.InvoiceShares;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlement.Invoice;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fatura kaydı ve dosya kapatma (analiz 5.11, KR-16). Fatura iplik başına kilo alır, kilolar fasoncunun bakiyesinden
 * düşer; kalan kilo bitmeden dosya açık kalır. Aynı fatura numarasıyla tekrarlanan kayıt ve kapanmış dosyaya
 * tekrarlanan kapatma başarılı sayılır; kayıt geri alınamaz.
 */
@Service
public class FireInvoiceService implements MarkInvoicedUseCase, CloseFireSettlementUseCase {

    private final ScopedShipmentLoader shipments;
    private final FireSettlementPort settlements;
    private final FireInvoicePort invoices;
    private final FireSettlementDetailsReader reader;
    private final LedgerEventPort events;
    private final Clock clock;

    public FireInvoiceService(
            ScopedShipmentLoader shipments,
            FireSettlementPort settlements,
            FireInvoicePort invoices,
            FireSettlementDetailsReader reader,
            LedgerEventPort events,
            Clock clock) {
        this.shipments = shipments;
        this.settlements = settlements;
        this.invoices = invoices;
        this.reader = reader;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FireSettlementDetails markInvoiced(MarkInvoicedCommand command) {
        FireSettlement current = lockedSettlement(command.shipmentId(), command.actor());
        String invoiceNo = command.invoiceNo().strip();
        if (invoices.invoiceExists(current.shipmentId(), invoiceNo)) {
            return reader.read(current);
        }
        current.requireOpen();
        FireSettlementDetails before = reader.read(current);
        Instant now = clock.instant();
        InvoiceRecord record = InvoiceShares.record(command, before, now);
        Invoice invoice =
                new Invoice(command.invoiceNo(), command.invoicedOn(), command.note(), InvoiceShares.totalKg(record));
        FireSettlement updated = current.markInvoiced(
                before.calculation(), invoice, command.actor().userId());
        invoices.recordInvoice(record);
        if (!updated.equals(current)) {
            settlements.save(updated, now);
        }
        events.fireInvoiced(current.shipmentId(), invoice, command.actor().userId(), now);
        return reader.read(updated);
    }

    @Override
    @Transactional
    public FireSettlementDetails closeFireSettlement(FireSettlementQuery query) {
        FireSettlement current = lockedSettlement(query.shipmentId(), query.actor());
        if (current.status() == FireSettlementStatus.CLOSED) {
            return reader.read(current);
        }
        FireSettlement closed = current.close(reader.read(current).calculation());
        Instant now = clock.instant();
        settlements.save(closed, now);
        events.fireSettlementClosed(closed, query.actor().userId(), now);
        return reader.read(closed);
    }

    private FireSettlement lockedSettlement(UUID shipmentId, Actor actor) {
        shipments.load(actor, shipmentId, Permission.INVOICE_MARK);
        return settlements.lock(shipmentId).orElseThrow(() -> new FireSettlementNotFoundException(shipmentId));
    }
}
