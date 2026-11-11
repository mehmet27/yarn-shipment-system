package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementPage;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort.FireTotals;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort.ListedSettlement;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Fire dosyasını kayıtlardan hesaplanan toplamlarla birleştirir (analiz 8.1); fire servisleri bunu paylaşır. */
@Component
public final class FireSettlementDetailsReader {

    private final FireSettlementPort settlements;

    public FireSettlementDetailsReader(FireSettlementPort settlements) {
        this.settlements = settlements;
    }

    /** Dosya ve güncel hesabı; toplamlar o anki kayıtlardan okunur. */
    public FireSettlementDetails read(FireSettlement settlement) {
        UUID shipmentId = settlement.shipmentId();
        return new FireSettlementDetails(settlement, calculate(settlement, settlements.totals(shipmentId)));
    }

    FireSettlementPage.Row toRow(ListedSettlement listed) {
        FireSettlement settlement = listed.settlement();
        return new FireSettlementPage.Row(
                settlement.shipmentId(),
                listed.shipmentNo(),
                listed.subcontractorTitle(),
                settlement.status(),
                calculate(settlement, listed.totals()));
    }

    private static FireCalculation calculate(FireSettlement settlement, FireTotals totals) {
        return FireCalculation.of(new Inputs(
                settlement.sentKg(), totals.returnedKg(), totals.claimKg(), totals.fabricKg(), settlement.fireRate()));
    }
}
