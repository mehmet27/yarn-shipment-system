package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.ScaleTicketNotOwnedException;
import com.yarnshipment.evidence.domain.model.ShipmentDepartedException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Kantar fişini yenileme kuralı: ilk fiş her zaman yüklenebilir (tartı kaydından hemen sonra gelir). Fiş zaten
 * varsa yenileme yalnızca sevkiyat yola çıkmadan ve yalnızca ilk fişi yükleyen çalışan tarafından yapılır.
 */
@Component
final class ScaleTicketRefresh {

    private final EvidenceStorePort evidence;

    ScaleTicketRefresh(EvidenceStorePort evidence) {
        this.evidence = evidence;
    }

    void require(Actor actor, ShipmentSnapshot shipment) {
        List<EvidenceFile> tickets = evidence.findByShipment(shipment.shipmentId()).stream()
                .filter(file -> file.declared().type() == EvidenceType.DISPATCH_SCALE)
                .toList();
        if (tickets.isEmpty()) {
            return;
        }
        if (shipment.departed()) {
            throw new ShipmentDepartedException(shipment.shipmentId());
        }
        if (tickets.stream().noneMatch(file -> file.uploadedBy().equals(actor.userId()))) {
            throw new ScaleTicketNotOwnedException(shipment.shipmentId());
        }
    }
}
