package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricReceiptsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptCommand;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnDistribution;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gelen kumaşı sevkiyata kaydeder (analiz 5.8). Yalnız tamamlanmış sevkiyata ve kapanmamış fire dosyasına girilir
 * (5.11). İplik cinsleri verilmezse kumaş, sevkiyatın reçete oranlarıyla dağıtılır; kayıt bakiyeden düşer ve fire
 * hesabına girer.
 */
@Service
public class FabricReceiptService implements RecordFabricReceiptUseCase, ListFabricReceiptsUseCase {

    private final ScopedShipmentLoader shipments;
    private final FabricReceiptPort receipts;
    private final FireSettlementPort settlements;
    private final BalanceQueryPort balances;
    private final Clock clock;

    public FabricReceiptService(
            ScopedShipmentLoader shipments,
            FabricReceiptPort receipts,
            FireSettlementPort settlements,
            BalanceQueryPort balances,
            Clock clock) {
        this.shipments = shipments;
        this.receipts = receipts;
        this.settlements = settlements;
        this.balances = balances;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FabricReceipt recordFabricReceipt(RecordFabricReceiptCommand command) {
        Shipment shipment = shipments.load(command.actor(), command.shipmentId(), Permission.FABRIC_RECEIPT_RECORD);
        if (shipment.status() != ShipmentStatus.COMPLETED) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.COMPLETED);
        }
        FireSettlement settlement =
                settlements.lock(shipment.id()).orElseThrow(() -> new FireSettlementNotFoundException(shipment.id()));
        settlement.requireOpen();
        Instant now = clock.instant();
        BigDecimal netKg = Kilograms.round(command.netKg());
        FabricReceipt receipt = new FabricReceipt(
                UUID.randomUUID(),
                shipment.id(),
                shipment.subcontractorId(),
                command.fabricType(),
                netKg,
                command.rollCount(),
                command.waybillNo(),
                command.receivedAt() == null ? now : command.receivedAt(),
                yarnsOf(command, shipment.id(), netKg),
                command.actor().userId());
        receipts.save(receipt);
        settlements.save(settlement.afterFabricReceipt(), now);
        return receipt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricReceipt> listFabricReceipts(ShipmentRecordsQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return receipts.findByShipment(query.shipmentId());
    }

    private List<YarnShare> yarnsOf(RecordFabricReceiptCommand command, UUID shipmentId, BigDecimal netKg) {
        List<YarnShare> given = command.yarns();
        if (given == null) {
            return YarnDistribution.proportional(netKg, balances.findDispatchedByYarn(shipmentId));
        }
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        given.forEach(share -> merged.merge(share.yarnType(), share.netKg(), BigDecimal::add));
        return merged.entrySet().stream()
                .map(entry -> new YarnShare(entry.getKey(), entry.getValue()))
                .toList();
    }
}
