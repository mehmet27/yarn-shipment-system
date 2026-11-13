package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.time.Instant;
import java.util.UUID;

/** Defter işlemlerinin olaylarını yayınlar (EVT-03); denetim kaydı bu olaylardan yazılır. */
public interface LedgerEventPort {

    void yarnReceiptRecorded(CentralStockMovement receipt);

    void centralProductionRecorded(CentralProduction production);

    void fabricReceived(FabricReceipt receipt);

    void fireRateChanged(FireSettlementPort.RateChange change);

    void fireInvoiced(FireSettlement settlement, UUID actorId, Instant at);

    void fireSettlementClosed(FireSettlement settlement, UUID actorId, Instant at);
}
