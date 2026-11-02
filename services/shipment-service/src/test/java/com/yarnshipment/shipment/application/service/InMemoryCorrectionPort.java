package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.application.port.out.CorrectionPort;
import com.yarnshipment.shipment.domain.correction.Correction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Kaydı bellekte tutan düzeltme portu; okunan kayıt veritabanındaki gibi onaylanmış döner. */
final class InMemoryCorrectionPort implements CorrectionPort {

    private final List<Correction> recorded = new ArrayList<>();
    private final Map<UUID, ShipmentCorrection> saved = new LinkedHashMap<>();

    List<Correction> recorded() {
        return recorded;
    }

    @Override
    public Optional<ShipmentCorrection> findCorrection(UUID correctionId) {
        return Optional.ofNullable(saved.get(correctionId));
    }

    @Override
    public List<ShipmentCorrection> findCorrections(UUID shipmentId) {
        return saved.values().stream()
                .filter(correction -> correction.shipmentId().equals(shipmentId))
                .toList();
    }

    @Override
    public void recordCorrection(Correction correction, UUID actorUserId) {
        recorded.add(correction);
        saved.put(
                correction.id(),
                new ShipmentCorrection(
                        correction.id(),
                        correction.shipmentId(),
                        correction.target(),
                        correction.oldValue().text(),
                        correction.newValue().text(),
                        correction.reason(),
                        "APPROVED",
                        actorUserId,
                        actorUserId,
                        correction.correctedAt()));
    }
}
