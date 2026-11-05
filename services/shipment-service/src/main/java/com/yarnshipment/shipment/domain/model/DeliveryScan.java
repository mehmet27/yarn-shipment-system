package com.yarnshipment.shipment.domain.model;

import com.yarnshipment.shipment.domain.packaging.PackageType;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Teslimde okutulan çuval anahtarı. Listede olmayan anahtar da saklanır.
 *
 * @param scannedKey okutulan anahtar
 * @param matched anahtar çıkış listesinde var mıydı
 * @param scanMethod okutma yöntemi
 * @param scannedAt okutma zamanı
 * @param packageType eşleşen çıkış çuvalının ambalaj türü; eşleşme yoksa {@code null}
 */
public record DeliveryScan(
        String scannedKey,
        boolean matched,
        ScanMethod scanMethod,
        Instant scannedAt,
        @Nullable PackageType packageType) {

    public DeliveryScan {
        Objects.requireNonNull(scannedKey, "scannedKey");
        Objects.requireNonNull(scanMethod, "scanMethod");
        Objects.requireNonNull(scannedAt, "scannedAt");
    }
}
