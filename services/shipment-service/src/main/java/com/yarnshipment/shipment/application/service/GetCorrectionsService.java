package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.correction.GetCorrectionsUseCase;
import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.application.port.out.CorrectionPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyatı kapsam kontrolünden geçirir (SEC-03), sonra düzeltme kayıtlarını okur. */
@Service
public class GetCorrectionsService implements GetCorrectionsUseCase {

    private final ScopedShipmentLoader shipments;
    private final CorrectionPort corrections;

    public GetCorrectionsService(ScopedShipmentLoader shipments, CorrectionPort corrections) {
        this.shipments = shipments;
        this.corrections = corrections;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentCorrection> getCorrections(GetShipmentHistoryQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return corrections.findCorrections(query.shipmentId());
    }
}
