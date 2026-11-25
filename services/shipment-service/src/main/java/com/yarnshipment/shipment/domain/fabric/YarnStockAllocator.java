package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bakiye defterinin sevkiyat, iplik ve parti başına toplamlarından kumaşa girebilecek kalan ipliği çıkarır. Partisiz
 * hareketler (dönen iplik, fatura gibi) bir partiye bağlı değildir: sevkiyatın partisiz kalanını aşan kısım, aynı
 * ipliğin partilerinden en eskisinden başlayarak düşer.
 */
public final class YarnStockAllocator {

    private YarnStockAllocator() {}

    /**
     * Defterden okunan bir toplam.
     *
     * @param netKg işaretli toplam: sevkiyatla gelen artı, düşenler eksi
     * @param firstAt satırın ilk hareketinin zamanı; parti sırası için
     */
    public record Bucket(
            UUID shipmentId,
            String shipmentNo,
            Instant since,
            String rawYarnType,
            String yarnName,
            @Nullable UUID lotId,
            @Nullable String lotNo,
            @Nullable String supplierName,
            BigDecimal netKg,
            Instant firstAt) {}

    public static List<YarnStockRow> allocate(List<Bucket> buckets) {
        Map<String, List<Bucket>> byYarn = new LinkedHashMap<>();
        for (Bucket bucket : buckets) {
            byYarn.computeIfAbsent(
                            bucket.shipmentId() + "|"
                                    + bucket.yarnName().strip().toLowerCase(Locale.ROOT),
                            key -> new ArrayList<>())
                    .add(bucket);
        }
        List<YarnStockRow> rows = new ArrayList<>();
        byYarn.values().forEach(group -> rows.addAll(allocateOne(group)));
        return rows;
    }

    private static List<YarnStockRow> allocateOne(List<Bucket> group) {
        BigDecimal deficit = BigDecimal.ZERO;
        List<Bucket> lotted = new ArrayList<>();
        List<YarnStockRow> rows = new ArrayList<>();
        for (Bucket bucket : group) {
            if (bucket.lotId() != null) {
                lotted.add(bucket);
            } else if (bucket.netKg().signum() < 0) {
                deficit = deficit.add(bucket.netKg().negate());
            } else {
                addRow(rows, bucket, bucket.netKg());
            }
        }
        lotted.sort(Comparator.comparing(Bucket::firstAt));
        for (Bucket bucket : lotted) {
            BigDecimal take = bucket.netKg().signum() > 0 ? bucket.netKg().min(deficit) : BigDecimal.ZERO;
            deficit = deficit.subtract(take);
            addRow(rows, bucket, bucket.netKg().subtract(take));
        }
        return rows;
    }

    private static void addRow(List<YarnStockRow> rows, Bucket bucket, BigDecimal kg) {
        BigDecimal rounded = Kilograms.round(kg);
        if (rounded.signum() > 0) {
            rows.add(new YarnStockRow(
                    bucket.shipmentId(),
                    bucket.shipmentNo(),
                    bucket.since(),
                    bucket.rawYarnType(),
                    bucket.yarnName(),
                    bucket.lotId(),
                    bucket.lotNo(),
                    bucket.supplierName(),
                    rounded));
        }
    }
}
