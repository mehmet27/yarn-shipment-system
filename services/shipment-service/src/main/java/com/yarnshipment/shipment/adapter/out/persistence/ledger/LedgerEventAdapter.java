package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.events.shipment.FabricReceived;
import com.yarnshipment.events.shipment.FireInvoiced;
import com.yarnshipment.events.shipment.FireRateChanged;
import com.yarnshipment.events.shipment.FireSettlementClosed;
import com.yarnshipment.events.stock.CentralProductionRecorded;
import com.yarnshipment.events.stock.YarnReceiptRecorded;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort.RateChange;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link LedgerEventPort}'un olay yayıncısı uygulaması; olaylar iş işlemiyle aynı transaction'da yazılır. */
@Component
final class LedgerEventAdapter implements LedgerEventPort {

    private static final String SHIPMENT = "Shipment";
    private static final String CENTRAL_STOCK = "CentralStock";

    private final DomainEventPublisher events;

    LedgerEventAdapter(DomainEventPublisher events) {
        this.events = events;
    }

    @Override
    public void yarnReceiptRecorded(CentralStockMovement receipt) {
        YarnReceiptRecorded event =
                new YarnReceiptRecorded(receipt.id(), receipt.yarnType(), receipt.netKg(), receipt.occurredAt());
        events.publish(new DomainEvent(
                CENTRAL_STOCK,
                receipt.id(),
                YarnReceiptRecorded.TYPE,
                event,
                receipt.occurredAt(),
                receipt.createdBy()));
    }

    @Override
    public void centralProductionRecorded(CentralProduction production) {
        CentralProductionRecorded event = new CentralProductionRecorded(
                production.id(),
                production.fabricType(),
                production.netKg(),
                production.fireRate(),
                production.producedAt());
        events.publish(new DomainEvent(
                CENTRAL_STOCK,
                production.id(),
                CentralProductionRecorded.TYPE,
                event,
                production.producedAt(),
                production.createdBy()));
    }

    @Override
    public void fabricReceived(FabricReceipt receipt) {
        FabricReceived event = new FabricReceived(
                receipt.shipmentId(), receipt.id(), receipt.fabricType(), receipt.netKg(), receipt.receivedAt());
        events.publish(new DomainEvent(
                SHIPMENT, receipt.shipmentId(), FabricReceived.TYPE, event, receipt.receivedAt(), receipt.createdBy()));
    }

    @Override
    public void fireRateChanged(RateChange change) {
        FireRateChanged event =
                new FireRateChanged(change.shipmentId(), change.oldRate(), change.newRate(), change.changedAt());
        events.publish(new DomainEvent(
                SHIPMENT, change.shipmentId(), FireRateChanged.TYPE, event, change.changedAt(), change.changedBy()));
    }

    @Override
    public void fireInvoiced(FireSettlement settlement, UUID actorId, Instant at) {
        FireInvoiced event = new FireInvoiced(
                settlement.shipmentId(),
                Objects.requireNonNull(settlement.invoiceNo(), "invoiceNo"),
                Objects.requireNonNull(settlement.invoicedOn(), "invoicedOn"),
                at);
        events.publish(new DomainEvent(SHIPMENT, settlement.shipmentId(), FireInvoiced.TYPE, event, at, actorId));
    }

    @Override
    public void fireSettlementClosed(FireSettlement settlement, UUID actorId, Instant at) {
        FireSettlementClosed event = new FireSettlementClosed(settlement.shipmentId(), at);
        events.publish(
                new DomainEvent(SHIPMENT, settlement.shipmentId(), FireSettlementClosed.TYPE, event, at, actorId));
    }
}
