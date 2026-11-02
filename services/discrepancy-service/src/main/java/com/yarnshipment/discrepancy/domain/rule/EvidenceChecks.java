package com.yarnshipment.discrepancy.domain.rule;

import com.yarnshipment.discrepancy.domain.model.ComparisonRule;
import com.yarnshipment.discrepancy.domain.model.Finding;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Fotoğraf kuralları (ön analiz, Bölüm 11.1 ve 12.1). Çıkışta çuvalların genel görünümü, kantar fişi, araç plakası ve
 * mühür fotoğrafı zorunludur; teslimde eksik olan her tür için ayrı uyuşmazlık açılır, tür sonradan yüklenince kapanır.
 * Teslim tarafının zorunlu kanıtları (kantar fişi, imzalı belge) teslim girişinde ve belge kuralında aranır.
 */
public final class EvidenceChecks {

    /** Çıkışta zorunlu kanıt türleri (evidence-service {@code EvidenceType} adlarıyla). */
    public static final List<String> REQUIRED_AT_DISPATCH =
            List.of("DISPATCH_SCALE", "BAG_OVERVIEW", "VEHICLE_PLATE", "SEAL");

    private EvidenceChecks() {}

    /** Yüklenmemiş her zorunlu tür için bir {@code PHOTO_MISSING} bulgusu; beklenen değer kanıt türüdür. */
    public static List<Finding> missing(Collection<String> uploadedTypes) {
        return REQUIRED_AT_DISPATCH.stream()
                .filter(type -> !uploadedTypes.contains(type))
                .map(type -> Finding.of(ComparisonRule.PHOTO_MISSING, type, null))
                .toList();
    }

    /**
     * Aynı dosya başka bir sevkiyatta daha önce yüklenmiş. Değerler yalnızca kanıt kimlikleridir; diğer sevkiyatın
     * numarası başka bir fasoncuya ait olabileceği için yazılmaz.
     *
     * @param evidenceId bu sevkiyata yeni yüklenen kanıt
     * @param duplicateOfId aynı dosyanın önceki kaydı
     */
    public static Finding reused(UUID evidenceId, UUID duplicateOfId) {
        return Finding.of(ComparisonRule.PHOTO_REUSED, duplicateOfId, evidenceId);
    }
}
