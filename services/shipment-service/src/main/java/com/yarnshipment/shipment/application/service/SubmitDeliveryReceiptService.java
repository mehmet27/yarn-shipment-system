package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptCommand;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptUseCase;
import com.yarnshipment.shipment.application.port.out.DeliveryCommandPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kör teslim girişi. Tutanak bir kez gönderilir; tekrar gelen istek yeni kayıt üretmeden 204 döner (API-04). */
@Service
public class SubmitDeliveryReceiptService implements SubmitDeliveryReceiptUseCase {

    private final ScopedShipmentLoader shipments;
    private final DeliveryCommandPort deliveryCommandPort;
    private final Clock clock;

    public SubmitDeliveryReceiptService(
            ScopedShipmentLoader shipments, DeliveryCommandPort deliveryCommandPort, Clock clock) {
        this.shipments = shipments;
        this.deliveryCommandPort = deliveryCommandPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void submitDeliveryReceipt(SubmitDeliveryReceiptCommand command) {
        Shipment shipment = shipments.load(command.actor(), command.shipmentId(), Permission.DELIVERY_RECORD);
        if (isRepeatedRequest(shipment)) {
            return;
        }
        deliveryCommandPort.recordDelivery(
                shipment.receiveDelivery(command.actor().userId(), command.measurement(), clock.instant()));
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.AWAITING_DELIVERY
                && deliveryCommandPort.hasDeliveryReceipt(shipment.id());
    }
}
