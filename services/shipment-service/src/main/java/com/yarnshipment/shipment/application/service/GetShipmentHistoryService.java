package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryUseCase;
import com.yarnshipment.shipment.application.port.out.ActorNamePort;
import com.yarnshipment.shipment.application.port.out.ShipmentHistoryPort;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyatı kapsam kontrolünden geçirir (SEC-03), sonra durum geçmişini okur. */
@Service
public class GetShipmentHistoryService implements GetShipmentHistoryUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentHistoryPort historyPort;
    private final ActorNamePort actorNames;

    public GetShipmentHistoryService(
            ScopedShipmentLoader shipments, ShipmentHistoryPort historyPort, ActorNamePort actorNames) {
        this.shipments = shipments;
        this.historyPort = historyPort;
        this.actorNames = actorNames;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusChange> getHistory(GetShipmentHistoryQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        List<StatusChange> history = historyPort.findHistory(query.shipmentId());
        Map<UUID, String> names = actorNames.namesOf(
                history.stream().map(StatusChange::changedBy).distinct().toList());
        return history.stream()
                .map(change -> change.withChangedByName(names.get(change.changedBy())))
                .toList();
    }
}
