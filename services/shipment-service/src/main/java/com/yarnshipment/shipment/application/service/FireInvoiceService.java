package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fire.CloseFireSettlementUseCase;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementQuery;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedCommand;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlement.Invoice;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Fatura edildi" işareti ve dosya kapatma (analiz 5.11, KR-16). Aynı faturayla tekrarlanan işaret ve kapanmış dosyaya
 * tekrarlanan kapatma başarılı sayılır; işaret geri alınamaz.
 */
@Service
public class FireInvoiceService implements MarkInvoicedUseCase, CloseFireSettlementUseCase {

    private final ScopedShipmentLoader shipments;
    private final FireSettlementPort settlements;
    private final FireSettlementDetailsReader reader;
    private final Clock clock;

    public FireInvoiceService(
            ScopedShipmentLoader shipments,
            FireSettlementPort settlements,
            FireSettlementDetailsReader reader,
            Clock clock) {
        this.shipments = shipments;
        this.settlements = settlements;
        this.reader = reader;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FireSettlementDetails markInvoiced(MarkInvoicedCommand command) {
        FireSettlement current = lockedSettlement(command.shipmentId(), command.actor());
        if (current.status() == FireSettlementStatus.INVOICED
                && command.invoiceNo().strip().equals(current.invoiceNo())) {
            return reader.read(current);
        }
        FireSettlement invoiced = current.markInvoiced(
                reader.read(current).calculation(),
                new Invoice(command.invoiceNo(), command.invoicedOn(), command.note()),
                command.actor().userId());
        settlements.save(invoiced, clock.instant());
        return reader.read(invoiced);
    }

    @Override
    @Transactional
    public FireSettlementDetails closeFireSettlement(FireSettlementQuery query) {
        FireSettlement current = lockedSettlement(query.shipmentId(), query.actor());
        if (current.status() == FireSettlementStatus.CLOSED) {
            return reader.read(current);
        }
        FireSettlement closed = current.close(reader.read(current).calculation());
        settlements.save(closed, clock.instant());
        return reader.read(closed);
    }

    private FireSettlement lockedSettlement(UUID shipmentId, Actor actor) {
        shipments.load(actor, shipmentId, Permission.INVOICE_MARK);
        return settlements.lock(shipmentId).orElseThrow(() -> new FireSettlementNotFoundException(shipmentId));
    }
}
