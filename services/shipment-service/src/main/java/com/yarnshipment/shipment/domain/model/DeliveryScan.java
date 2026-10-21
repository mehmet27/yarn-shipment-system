package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Teslimde okutulan çuval anahtarı. Listede olmayan anahtar da saklanır.
 *
 * @param scannedKey okutulan anahtar
 * @param matched anahtar çıkış listesinde var mıydı
 * @param scanMethod okutma yöntemi
 * @param scannedAt okutma zamanı
 */
public record DeliveryScan(String scannedKey, boolean matched, ScanMethod scanMethod, Instant scannedAt) {

    public DeliveryScan {
        Objects.requireNonNull(scannedKey, "scannedKey");
        Objects.requireNonNull(scanMethod, "scanMethod");
        Objects.requireNonNull(scannedAt, "scannedAt");
    }
}
