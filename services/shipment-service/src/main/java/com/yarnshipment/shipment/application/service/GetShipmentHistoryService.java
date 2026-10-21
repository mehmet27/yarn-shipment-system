package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentHistoryPort;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyatı kapsam kontrolünden geçirir (SEC-03), sonra durum geçmişini okur. */
@Service
public class GetShipmentHistoryService implements GetShipmentHistoryUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentHistoryPort historyPort;

    public GetShipmentHistoryService(ScopedShipmentLoader shipments, ShipmentHistoryPort historyPort) {
        this.shipments = shipments;
        this.historyPort = historyPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusChange> getHistory(GetShipmentHistoryQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return historyPort.findHistory(query.shipmentId());
    }
}
