package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fabric.FabricDrawEntry;
import com.yarnshipment.shipment.application.port.in.fabric.FabricYarnOption;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricDrawsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricYarnOptionsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.PreviewFabricReceiptCommand;
import com.yarnshipment.shipment.application.port.in.fabric.PreviewFabricReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.application.port.out.ledger.YarnStockPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner.Plan;
import com.yarnshipment.shipment.domain.fabric.YarnStockRow;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kumaş girişinin okuma tarafı: fasoncunun elindeki iplikler, kaydetmeden önce düşüm önizlemesi ve bir sevkiyatın
 * ipliğinden üretilen kumaşlar. Hiçbir şey yazmaz.
 */
@Service
public class FabricYarnQueryService
        implements PreviewFabricReceiptUseCase, ListFabricYarnOptionsUseCase, ListFabricDrawsUseCase {

    private final ScopedShipmentLoader shipments;
    private final FabricReceiptPort receipts;
    private final YarnStockPort stock;

    public FabricYarnQueryService(ScopedShipmentLoader shipments, FabricReceiptPort receipts, YarnStockPort stock) {
        this.shipments = shipments;
        this.receipts = receipts;
        this.stock = stock;
    }

    @Override
    @Transactional(readOnly = true)
    public Plan previewFabricReceipt(PreviewFabricReceiptCommand command) {
        Shipment shipment = FabricReceiptService.requireCompleted(
                shipments.load(command.actor(), command.shipmentId(), Permission.FABRIC_RECEIPT_RECORD));
        return YarnDrawPlanner.plan(
                shipment.id(),
                Kilograms.round(command.netKg()),
                command.yarns(),
                stock.findOpenStock(shipment.subcontractorId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricYarnOption> listFabricYarnOptions(ShipmentRecordsQuery query) {
        Shipment shipment = shipments.load(query.actor(), query.shipmentId(), Permission.FABRIC_RECEIPT_RECORD);
        Map<String, FabricYarnOption> options = new LinkedHashMap<>();
        for (YarnStockRow row : stock.findOpenStock(shipment.subcontractorId())) {
            BigDecimal own = row.shipmentId().equals(shipment.id()) ? row.availableKg() : Kilograms.ZERO;
            options.merge(
                    row.yarnName().strip().toLowerCase(Locale.ROOT) + '|' + row.lotId(),
                    new FabricYarnOption(
                            row.yarnName(), row.lotId(), row.lotNo(), row.supplierName(), row.availableKg(), own),
                    FabricYarnQueryService::sum);
        }
        return List.copyOf(options.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricDrawEntry> listFabricDraws(ShipmentRecordsQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return receipts.findDrawsFrom(query.shipmentId());
    }

    private static FabricYarnOption sum(FabricYarnOption left, FabricYarnOption right) {
        return new FabricYarnOption(
                left.yarnType(),
                left.lotId(),
                left.lotNo(),
                left.supplierName(),
                left.totalKg().add(right.totalKg()),
                left.ownKg().add(right.ownKg()));
    }
}
