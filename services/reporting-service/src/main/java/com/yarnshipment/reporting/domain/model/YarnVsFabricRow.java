package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** Fasoncu bazında giden iplik ↔ gelen kumaş toplamı. */
public record YarnVsFabricRow(
        UUID subcontractorId,
        String subcontractorName,
        BigDecimal sentKg,
        BigDecimal returnedYarnKg,
        BigDecimal fabricKg,
        BigDecimal actualFireRate,
        BigDecimal shortageKg) {

    /** Dosyaları fasoncuya göre toplar; fire oranı toplamlardan, eksik kilo dosya eksiklerinin toplamından gelir. */
    public static List<YarnVsFabricRow> aggregate(List<FireFile> files) {
        return files.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        FireFile::subcontractorId, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .values()
                .stream()
                .map(YarnVsFabricRow::of)
                .toList();
    }

    private static YarnVsFabricRow of(List<FireFile> group) {
        BigDecimal sent = sum(group, FireFile::sentKg);
        BigDecimal returned = sum(group, FireFile::returnedYarnKg);
        BigDecimal fabric = sum(group, FireFile::fabricKg);
        BigDecimal shortage = sum(group, FireFile::shortageKg);
        return new YarnVsFabricRow(
                group.get(0).subcontractorId(),
                group.get(0).subcontractorName(),
                sent,
                returned,
                fabric,
                FireFile.actualFireRate(sent.subtract(returned), fabric),
                shortage.setScale(FireFile.KG_SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal sum(List<FireFile> files, java.util.function.Function<FireFile, BigDecimal> getter) {
        return files.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
