package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Karşılaştırmanın sonucu.
 *
 * @param shipmentId sevkiyat
 * @param quantityBand net kilo farkının bandı; çıkış tartısı yoksa Kırmızı
 * @param diffKg çıkış net − teslim net; çıkış tartısı yoksa boş
 * @param diffPct fark yüzdesi (iki ondalık); çıkış tartısı yoksa boş
 * @param findings açılacak uyuşmazlıklar
 */
public record ComparisonResult(
        UUID shipmentId,
        Band quantityBand,
        @Nullable BigDecimal diffKg,
        @Nullable BigDecimal diffPct,
        List<Finding> findings) {

    public ComparisonResult {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(quantityBand, "quantityBand");
        findings = List.copyOf(findings);
    }

    /** Bu sonuca ek bulgular, ör. kantar fişi kontrolleri, eklenmiş hali. */
    public ComparisonResult plus(List<Finding> extra) {
        List<Finding> all = new ArrayList<>(findings);
        all.addAll(extra);
        return new ComparisonResult(shipmentId, quantityBand, diffKg, diffPct, all);
    }

    /** Uyuşmazlık yoksa sevkiyat tamamlanır. */
    public boolean clean() {
        return findings.isEmpty();
    }

    public Optional<Severity> highestSeverity() {
        return findings.stream().map(Finding::severity).max(Comparator.naturalOrder());
    }
}
