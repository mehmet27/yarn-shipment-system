package com.yarnshipment.shipment.masterdata.domain.catalog;

import java.util.Objects;
import java.util.UUID;

/**
 * Kalite sınıfı (A, B ...). Kod partinin kilidiyle çuvala kopyalanır.
 *
 * @param id kimlik
 * @param code değişmez kod; büyük harf
 * @param details ad ve etiket rengi
 * @param active listelerde görünür mü; pasif sınıf yeni partiye verilemez
 */
public record QualityGrade(UUID id, String code, QualityGradeDetails details, boolean active) {

    public QualityGrade {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(details, "details");
    }
}
