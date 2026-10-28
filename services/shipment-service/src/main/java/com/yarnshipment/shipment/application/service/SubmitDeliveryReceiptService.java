package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptCommand;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptUseCase;
import com.yarnshipment.shipment.application.port.out.DeliveryCommandPort;
import com.yarnshipment.shipment.application.port.out.EvidenceLookupPort;
import com.yarnshipment.shipment.domain.evidence.InvalidDeliveryEvidenceException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kör teslim girişi. Tutanak bir kez gönderilir; tekrar gelen istek yeni kayıt üretmeden 204 döner (API-04). */
@Service
public class SubmitDeliveryReceiptService implements SubmitDeliveryReceiptUseCase {

    private final ScopedShipmentLoader shipments;
    private final DeliveryCommandPort deliveryCommandPort;
    private final EvidenceLookupPort evidence;
    private final Clock clock;

    public SubmitDeliveryReceiptService(
            ScopedShipmentLoader shipments,
            DeliveryCommandPort deliveryCommandPort,
            EvidenceLookupPort evidence,
            Clock clock) {
        this.shipments = shipments;
        this.deliveryCommandPort = deliveryCommandPort;
        this.evidence = evidence;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void submitDeliveryReceipt(SubmitDeliveryReceiptCommand command) {
        Shipment shipment = shipments.load(command.actor(), command.shipmentId(), Permission.DELIVERY_RECORD);
        if (isRepeatedRequest(shipment)) {
            return;
        }
        requireValidSignedDocument(
                shipment.id(), command.measurement().document().signedDocumentEvidenceId());
        deliveryCommandPort.recordDelivery(
                shipment.receiveDelivery(command.actor().userId(), command.measurement(), clock.instant()));
    }

    /** Eklenen imzalı irsaliye, bu sevkiyata ait ve türü doğru bir kanıt olmalı; rastgele kimlik kabul edilmez. */
    private void requireValidSignedDocument(UUID shipmentId, @Nullable UUID evidenceId) {
        if (evidenceId == null) {
            return;
        }
        boolean valid = evidence.findEvidence(evidenceId)
                .filter(found -> found.isSignedDocumentOf(shipmentId))
                .isPresent();
        if (!valid) {
            throw new InvalidDeliveryEvidenceException(shipmentId, evidenceId);
        }
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.AWAITING_DELIVERY
                && deliveryCommandPort.hasDeliveryReceipt(shipment.id());
    }
}
