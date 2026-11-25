package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricReceiptsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptCommand;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.application.port.out.ledger.YarnStockPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.FabricYarnShortageException;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner.Plan;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gelen kumaşı bir sevkiyat üzerinden kaydeder (analiz 5.8). Kumaş tek iplikten ve tek sevkiyattan olmaz: kaliteci her
 * ipliğe oran yazar (toplam 100), iplikler fasoncunun açık sevkiyatlarından düşer. Her iplik önce kumaşın girildiği
 * sevkiyattan, orada yoksa ya da yetmiyorsa en eski sevkiyattan başlayarak düşer; fasoncuda yeterli iplik yoksa kayıt
 * engellenir. İpliği düşülen her sevkiyatın fire dosyası kumaşı hesaba katar.
 */
@Service
public class FabricReceiptService implements RecordFabricReceiptUseCase, ListFabricReceiptsUseCase {

    private final ScopedShipmentLoader shipments;
    private final FabricReceiptPort receipts;
    private final FireSettlementPort settlements;
    private final YarnStockPort stock;
    private final Clock clock;

    public FabricReceiptService(
            ScopedShipmentLoader shipments,
            FabricReceiptPort receipts,
            FireSettlementPort settlements,
            YarnStockPort stock,
            Clock clock) {
        this.shipments = shipments;
        this.receipts = receipts;
        this.settlements = settlements;
        this.stock = stock;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FabricReceipt recordFabricReceipt(RecordFabricReceiptCommand command) {
        Shipment shipment = requireCompleted(
                shipments.load(command.actor(), command.shipmentId(), Permission.FABRIC_RECEIPT_RECORD));
        // Kilitler sevkiyat kimliği sırasıyla alınır; aynı fasoncuya aynı anda giren iki kumaş birbirini bekler.
        Map<UUID, FireSettlement> open = lockOpen(shipment);
        requireOpenEntry(shipment, open);
        Instant now = clock.instant();
        if (command.receivedAt() != null) {
            FabricReceipt.requireNotInFuture(command.receivedAt(), now);
        }
        Plan plan = YarnDrawPlanner.plan(
                shipment.id(),
                Kilograms.round(command.netKg()),
                command.yarns(),
                stock.findOpenStock(shipment.subcontractorId()));
        if (!plan.isFeasible()) {
            throw new FabricYarnShortageException(plan.shortages());
        }
        FabricReceipt receipt = receiptOf(command, shipment, plan, now);
        receipts.save(receipt);
        for (UUID sourceId : plan.sourceShipmentIds()) {
            FireSettlement source = Objects.requireNonNull(open.get(sourceId), "source settlement");
            settlements.save(source.afterFabricReceipt(), now);
        }
        return receipt;
    }

    private static FabricReceipt receiptOf(
            RecordFabricReceiptCommand command, Shipment shipment, Plan plan, Instant now) {
        return new FabricReceipt(
                UUID.randomUUID(),
                shipment.id(),
                shipment.subcontractorId(),
                command.fabricType(),
                Kilograms.round(command.netKg()),
                command.rollCount(),
                command.waybillNo(),
                command.receivedAt() == null ? now : command.receivedAt(),
                plan.yarns(),
                command.actor().userId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricReceipt> listFabricReceipts(ShipmentRecordsQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return receipts.findByShipment(query.shipmentId());
    }

    /** Kumaş yalnız tamamlanmış sevkiyat üzerinden girilir. */
    static Shipment requireCompleted(Shipment shipment) {
        if (shipment.status() != ShipmentStatus.COMPLETED) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.COMPLETED);
        }
        return shipment;
    }

    private Map<UUID, FireSettlement> lockOpen(Shipment shipment) {
        Map<UUID, FireSettlement> open = new LinkedHashMap<>();
        settlements.lockOpenOf(shipment.subcontractorId()).forEach(item -> open.put(item.shipmentId(), item));
        return open;
    }

    /** Kumaşın girildiği sevkiyatın dosyası yoksa 404, kapanmışsa 409. */
    private void requireOpenEntry(Shipment shipment, Map<UUID, FireSettlement> open) {
        if (open.containsKey(shipment.id())) {
            return;
        }
        FireSettlement entry =
                settlements.find(shipment.id()).orElseThrow(() -> new FireSettlementNotFoundException(shipment.id()));
        entry.requireOpen();
    }
}
