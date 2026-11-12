package com.yarnshipment.shipment.masterdata.domain.catalog;

import java.util.Objects;
import java.util.UUID;

/**
 * Kalite sınıfı (A, B ...). Kod partiye bağlanır ve eklendikten sonra değişmez.
 *
 * @param id kimlik
 * @param code değişmez kod; büyük harf
 * @param details ad ve sıra
 * @param active listelerde görünür mü; pasif sınıf yeni partiye verilemez
 * @param lotCount sınıfın verildiği parti sayısı (pasif partiler dahil)
 */
public record QualityGrade(UUID id, String code, QualityGradeDetails details, boolean active, long lotCount) {

    public QualityGrade {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(details, "details");
    }
}
