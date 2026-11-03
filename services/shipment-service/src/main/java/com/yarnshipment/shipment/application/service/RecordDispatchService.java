package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import com.yarnshipment.shipment.application.port.out.DispatchCommandPort;
import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.application.port.out.TareBasisPort;
import com.yarnshipment.shipment.domain.model.InvalidDispatchException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.TareBasis;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depo çıkış tartımı ve mühür kaydı. Sevkiyat DRAFT'tan çıktıysa ve tartım zaten varsa ikinci istek yeni kayıt
 * üretmez, ilk sonuç (204) tekrar döner (API-04). Lokasyonun çuval standardı varsa tartıma kopyalanır; sapma uyarısı
 * kaydı engellemez. Tartılan çuval sayısı
 * sevkiyata atanmış çuval sayısına eşit olmalıdır.
 */
@Service
public class RecordDispatchService implements RecordDispatchUseCase {

    private final ScopedShipmentLoader shipments;
    private final DispatchCommandPort dispatchCommandPort;
    private final ShipmentBagsPort bagsPort;
    private final TareBasisPort tareBasisPort;
    private final Clock clock;

    public RecordDispatchService(
            ScopedShipmentLoader shipments,
            DispatchCommandPort dispatchCommandPort,
            ShipmentBagsPort bagsPort,
            TareBasisPort tareBasisPort,
            Clock clock) {
        this.shipments = shipments;
        this.dispatchCommandPort = dispatchCommandPort;
        this.bagsPort = bagsPort;
        this.tareBasisPort = tareBasisPort;
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
        TareBasis tareBasis = tareBasisPort.findBagTareBasis(
                shipment.siteId(), command.measurement().bagCount());
        dispatchCommandPort.recordDispatch(
                shipment.recordDispatch(command.measurement(), clock.instant(), tareBasis), actor.userId());
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.DRAFT && dispatchCommandPort.hasDispatchWeighing(shipment.id());
    }
}
