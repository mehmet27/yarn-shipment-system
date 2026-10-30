package com.yarnshipment.shipment.masterdata.domain.lot;

import com.yarnshipment.shipment.masterdata.domain.catalog.MasterDataCode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Partinin kilitten önce düzenlenebilen alanları. Lokasyon yalnızca eklerken verilir.
 *
 * @param lotNo parti numarası; lokasyonda tektir
 * @param productId iplik
 * @param qualityGradeId kalite sınıfı
 * @param productionDate üretim tarihi
 */
public record LotDetails(String lotNo, UUID productId, UUID qualityGradeId, LocalDate productionDate) {

    public LotDetails {
        lotNo = MasterDataCode.normalize("lotNo", lotNo);
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(qualityGradeId, "qualityGradeId");
        Objects.requireNonNull(productionDate, "productionDate");
    }
}
