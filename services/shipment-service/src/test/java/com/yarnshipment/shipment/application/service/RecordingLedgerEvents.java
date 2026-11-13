package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Yayınlanan defter olaylarının adlarını sırayla tutar. */
final class RecordingLedgerEvents implements LedgerEventPort {

    final List<String> names = new ArrayList<>();

    @Override
    public void yarnReceiptRecorded(CentralStockMovement receipt) {
        names.add("yarnReceipt");
    }

    @Override
    public void centralProductionRecorded(CentralProduction production) {
        names.add("centralProduction");
    }

    @Override
    public void fabricReceived(FabricReceipt receipt) {
        names.add("fabricReceived");
    }

    @Override
    public void fireRateChanged(UUID shipmentId, BigDecimal oldRate, BigDecimal newRate, UUID actorId, Instant at) {
        names.add("fireRateChanged");
    }

    @Override
    public void fireInvoiced(FireSettlement settlement, UUID actorId, Instant at) {
        names.add("fireInvoiced");
    }

    @Override
    public void fireSettlementClosed(FireSettlement settlement, UUID actorId, Instant at) {
        names.add("fireSettlementClosed");
    }
}
