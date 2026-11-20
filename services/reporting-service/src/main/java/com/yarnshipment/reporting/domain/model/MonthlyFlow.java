package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

/**
 * Bir takvim ayında tamamlanan sevkiyatların giden iplik ↔ gelen kumaş toplamı; aylık eğilim grafiğinin bir sütunu.
 * Tanımlar "Giden iplik ↔ gelen kumaş" raporuyla aynıdır ({@link YarnVsFabricRow}): aynı ay seçildiğinde o raporun
 * fasoncu satırlarının toplamına eşittir.
 *
 * @param month takvim ayı (Europe/Istanbul)
 * @param shipmentCount ayda tamamlanan sevkiyat (fire dosyası) sayısı
 * @param sentKg gönderilen iplik
 * @param returnedYarnKg dönen iplik
 * @param fabricKg gelen kumaş
 * @param actualFireRate gerçekleşen fire, kesir (0,0400); toplamlardan hesaplanır
 * @param shortageKg dosya eksiklerinin toplamı
 */
public record MonthlyFlow(
        YearMonth month,
        int shipmentCount,
        BigDecimal sentKg,
        BigDecimal returnedYarnKg,
        BigDecimal fabricKg,
        BigDecimal actualFireRate,
        BigDecimal shortageKg) {

    /** Ayın dosyalarını toplar; fire oranı {@link YarnVsFabricRow} ile aynı formülle toplamlardan gelir. */
    public static MonthlyFlow of(YearMonth month, List<FireFile> files) {
        BigDecimal sent = sum(files, FireFile::sentKg);
        BigDecimal returned = sum(files, FireFile::returnedYarnKg);
        BigDecimal fabric = sum(files, FireFile::fabricKg);
        return new MonthlyFlow(
                month,
                files.size(),
                kg(sent),
                kg(returned),
                kg(fabric),
                FireFile.actualFireRate(sent.subtract(returned), fabric.add(sum(files, FireFile::invoicedKg))),
                kg(sum(files, FireFile::shortageKg)));
    }

    private static BigDecimal kg(BigDecimal value) {
        return value.setScale(FireFile.KG_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<FireFile> files, Function<FireFile, BigDecimal> getter) {
        return files.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
