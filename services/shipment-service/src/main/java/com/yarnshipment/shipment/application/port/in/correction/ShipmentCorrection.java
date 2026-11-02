package com.yarnshipment.shipment.application.port.in.correction;

import com.yarnshipment.shipment.domain.correction.CorrectionTarget;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kaydedilmiş bir düzeltme.
 *
 * @param id düzeltme kaydı
 * @param shipmentId sevkiyat
 * @param target düzeltilen kayıt
 * @param oldValue eski değer
 * @param newValue yeni değer
 * @param reason gerekçe
 * @param status {@code PENDING}, {@code APPROVED} ya da {@code REJECTED}
 * @param requestedBy düzeltmeyi isteyen
 * @param approvedBy onaylayan Admin; bekleyen kayıtta boş
 * @param createdAt kayıt zamanı
 */
public record ShipmentCorrection(
        UUID id,
        UUID shipmentId,
        CorrectionTarget target,
        @Nullable String oldValue,
        @Nullable String newValue,
        String reason,
        String status,
        UUID requestedBy,
        @Nullable UUID approvedBy,
        Instant createdAt) {}
