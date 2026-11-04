package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Standart daranın değerleri.
 *
 * @param standardTareKg ambalaj başına standart dara (kg); sıfır, dara karşılaştırmasını kapatır
 * @param maxDeviationPct izin verilen en çok sapma, yüzde 0-100
 */
public record TareStandardDetails(BigDecimal standardTareKg, BigDecimal maxDeviationPct) {

    private static final BigDecimal MAX_PCT = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_KG = new BigDecimal("999999999.999");

    public TareStandardDetails {
        Objects.requireNonNull(standardTareKg, "standardTareKg");
        Objects.requireNonNull(maxDeviationPct, "maxDeviationPct");
        if (standardTareKg.signum() < 0 || standardTareKg.compareTo(MAX_KG) > 0) {
            throw new InvalidMasterDataException("standardTareKg", "out of range");
        }
        if (maxDeviationPct.signum() < 0 || maxDeviationPct.compareTo(MAX_PCT) > 0) {
            throw new InvalidMasterDataException("maxDeviationPct", "out of range");
        }
    }
}
