package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.domain.correction.Correction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Düzeltme kaydı ve düzeltilen satırın yeni sürümü aynı transaction'da yazılır. */
public interface CorrectionPort {

    Optional<ShipmentCorrection> findCorrection(UUID correctionId);

    List<ShipmentCorrection> findCorrections(UUID shipmentId);

    /**
     * Düzeltme kaydını onaylanmış olarak yazar, yeni satırı ekler ve eskisini {@code superseded_by} ile işaretler.
     * Geçerli satır okunduğundan beri değiştiyse {@code CorrectionConflictException}, düzeltilen mühür numarası başka
     * sevkiyatın çıkışında kullanılıyorsa {@code SealAlreadyUsedException}.
     */
    void recordCorrection(Correction correction, UUID actorUserId);
}
