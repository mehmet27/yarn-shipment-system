package com.yarnshipment.shipment.masterdata.domain.lot;

import com.yarnshipment.shipment.masterdata.domain.catalog.MasterDataCode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Partinin kilitten önce düzenlenebilen alanları. Lokasyon yalnızca eklerken verilir.
 *
 * @param lotNo parti numarası; lokasyonda tektir
 * @param productId iplik
 * @param qualityGradeId kalite sınıfı
 * @param productionDate üretim tarihi
 * @param yarnSupplierId iplik tedarikçisi; isteğe bağlı
 */
public record LotDetails(
        String lotNo,
        UUID productId,
        UUID qualityGradeId,
        LocalDate productionDate,
        @Nullable UUID yarnSupplierId) {

    /** Tedarikçisiz parti. */
    public LotDetails(String lotNo, UUID productId, UUID qualityGradeId, LocalDate productionDate) {
        this(lotNo, productId, qualityGradeId, productionDate, null);
    }

    public LotDetails {
        lotNo = MasterDataCode.normalize("lotNo", lotNo);
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(qualityGradeId, "qualityGradeId");
        Objects.requireNonNull(productionDate, "productionDate");
    }
}
