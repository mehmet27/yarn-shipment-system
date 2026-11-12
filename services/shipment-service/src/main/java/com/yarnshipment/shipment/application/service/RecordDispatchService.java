package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import com.yarnshipment.shipment.application.port.out.DispatchCommandPort;
import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.domain.model.InvalidDispatchException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depo çıkış tartımı; sevkiyat teslim bekliyor durumuna geçer. Net, beklenen net'ten kantar toleransından fazla
 * saparsa sevkiyat bunun yerine WEIGHT_MISMATCH olur ve müdür karar verir. Sevkiyat DRAFT'tan çıktıysa ve tartım
 * zaten varsa ikinci istek yeni kayıt üretmez, ilk sonuç (204) tekrar döner (API-04). Tartılan çuval sayısı
 * sevkiyata atanmış çuval sayısına eşit olmalıdır.
 */
@Service
public class RecordDispatchService implements RecordDispatchUseCase {

    private final ScopedShipmentLoader shipments;
    private final DispatchCommandPort dispatchCommandPort;
    private final ShipmentBagsPort bagsPort;
    private final ShipmentSettingsPort settingsPort;
    private final Clock clock;

    public RecordDispatchService(
            ScopedShipmentLoader shipments,
            DispatchCommandPort dispatchCommandPort,
            ShipmentBagsPort bagsPort,
            ShipmentSettingsPort settingsPort,
            Clock clock) {
        this.shipments = shipments;
        this.dispatchCommandPort = dispatchCommandPort;
        this.bagsPort = bagsPort;
        this.settingsPort = settingsPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recordDispatch(RecordDispatchCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DISPATCH_RECORD);
        if (isRepeatedRequest(shipment)) {
            return;
        }
        if (shipment.status() == ShipmentStatus.DRAFT
                && bagsPort.findDispatchBags(shipment.id()).size()
                        != command.measurement().bagCount()) {
            throw new InvalidDispatchException("bagCount", "differs from assigned bags");
        }
        dispatchCommandPort.recordDispatch(
                shipment.recordDispatch(
                        command.measurement(),
                        clock.instant(),
                        settingsPort.find().weightToleranceKg()),
                actor.userId());
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.DRAFT && dispatchCommandPort.hasDispatchWeighing(shipment.id());
    }
}
