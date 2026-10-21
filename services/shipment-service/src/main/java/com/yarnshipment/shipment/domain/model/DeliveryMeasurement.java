package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Fasoncunun teslimde girdiği değerler (kör giriş, SEC-05).
 *
 * @param grossKg brüt kilo
 * @param tareKg dara
 * @param bagCount tartılan çuval sayısı
 * @param sealCondition mührün durumu
 * @param sealNo mühür üzerindeki numara; mühür yoksa {@code null}
 * @param scannedKeys okutulan çuval anahtarları
 * @param scanMethod anahtarların giriş yolu
 * @param document irsaliye değerleri
 */
public record DeliveryMeasurement(
        BigDecimal grossKg,
        BigDecimal tareKg,
        int bagCount,
        SealCondition sealCondition,
        @Nullable String sealNo,
        Set<String> scannedKeys,
        ScanMethod scanMethod,
        DeliveryDocument document) {

    public DeliveryMeasurement {
        Objects.requireNonNull(grossKg, "grossKg");
        Objects.requireNonNull(tareKg, "tareKg");
        Objects.requireNonNull(sealCondition, "sealCondition");
        Objects.requireNonNull(scanMethod, "scanMethod");
        Objects.requireNonNull(document, "document");
        scannedKeys = Set.copyOf(scannedKeys);
        if (grossKg.compareTo(tareKg) < 0) {
            throw new InvalidDeliveryException("tareKg", "greater than gross");
        }
        if (bagCount <= 0) {
            throw new InvalidDeliveryException("bagCount", "not positive");
        }
        if (sealNo == null && sealCondition != SealCondition.MISSING) {
            throw new InvalidDeliveryException("sealNo", "required unless seal is missing");
        }
    }
}
