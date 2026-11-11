package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementPage;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementQuery;
import com.yarnshipment.shipment.application.port.in.fire.GetFireSettlementUseCase;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyatın fire dosyasını ve dosya listesini okur (analiz 5.10). */
@Service
public class FireSettlementQueryService implements GetFireSettlementUseCase, ListFireSettlementsUseCase {

    private final ScopedShipmentLoader shipments;
    private final FireSettlementPort settlements;
    private final FireSettlementDetailsReader reader;

    public FireSettlementQueryService(
            ScopedShipmentLoader shipments, FireSettlementPort settlements, FireSettlementDetailsReader reader) {
        this.shipments = shipments;
        this.settlements = settlements;
        this.reader = reader;
    }

    @Override
    @Transactional(readOnly = true)
    public FireSettlementDetails getFireSettlement(FireSettlementQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return settlements
                .find(query.shipmentId())
                .map(reader::read)
                .orElseThrow(() -> new FireSettlementNotFoundException(query.shipmentId()));
    }

    @Override
    @Transactional(readOnly = true)
    public FireSettlementPage listFireSettlements(ListFireSettlementsQuery query) {
        List<FireSettlementPage.Row> rows =
                settlements.findSettlements(query).stream().map(reader::toRow).toList();
        return new FireSettlementPage(rows, query.page(), query.size(), settlements.countSettlements(query));
    }
}
