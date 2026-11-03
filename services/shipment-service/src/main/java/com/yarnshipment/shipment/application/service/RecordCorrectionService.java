package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionCommand;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionUseCase;
import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.application.port.out.CorrectionPort;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.InvalidCorrectionException;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sevkiyatı kapsam kontrolüyle yükler (SEC-03), düzeltmeyi alan kurallarından geçirir ve yazar. Tekrar anahtarı
 * düzeltme kaydının kimliğidir; aynı anahtarla gelen ikinci istek ilk kaydı döner.
 */
@Service
public class RecordCorrectionService implements RecordCorrectionUseCase {

    private final ScopedShipmentLoader shipments;
    private final CorrectionPort corrections;
    private final Clock clock;

    public RecordCorrectionService(ScopedShipmentLoader shipments, CorrectionPort corrections, Clock clock) {
        this.shipments = shipments;
        this.corrections = corrections;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ShipmentCorrection recordCorrection(RecordCorrectionCommand command) {
        Shipment shipment = shipments.load(command.actor(), command.shipmentId(), Permission.CORRECTION_CREATE);
        Optional<ShipmentCorrection> repeated = corrections.findCorrection(command.idempotencyKey());
        if (repeated.isPresent()) {
            if (!repeated.get().shipmentId().equals(shipment.id())) {
                throw new InvalidCorrectionException("idempotencyKey");
            }
            return repeated.get();
        }
        Correction correction = Correction.of(shipment, command.request(), command.idempotencyKey(), clock.instant());
        corrections.recordCorrection(correction, command.actor().userId());
        return corrections.findCorrection(correction.id()).orElseThrow();
    }
}
