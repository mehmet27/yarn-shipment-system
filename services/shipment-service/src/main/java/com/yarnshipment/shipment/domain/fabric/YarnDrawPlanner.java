package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Kumaş kilosunu oranlara göre iplik kilolarına böler ve her ipliği sevkiyatlardan düşer. Her iplik önce kumaşın
 * girildiği sevkiyattan düşer; orada yoksa ya da yetmiyorsa kalan kısım ipliğin bulunduğu en eski sevkiyattan
 * başlayarak düşer. Fasoncunun toplamında yetmeyen iplik eksik olarak döner.
 */
public final class YarnDrawPlanner {

    private YarnDrawPlanner() {}

    /**
     * Düşüm planı.
     *
     * @param yarns oranına göre kilosu ve sevkiyat dağılımı hesaplanmış iplikler
     * @param shortages fasoncuda yetmeyen iplikler; boşsa plan uygulanabilir
     */
    public record Plan(List<FabricYarn> yarns, List<YarnShortage> shortages) {

        public Plan {
            yarns = List.copyOf(yarns);
            shortages = List.copyOf(shortages);
        }

        public boolean isFeasible() {
            return shortages.isEmpty();
        }

        /** İpliği düşülen sevkiyatlar, ilk düşüm sırasıyla. */
        public Set<UUID> sourceShipmentIds() {
            Set<UUID> ids = new LinkedHashSet<>();
            yarns.forEach(yarn -> yarn.draws().forEach(draw -> ids.add(draw.shipmentId())));
            return ids;
        }
    }

    /**
     * Planı kurar.
     *
     * @param entryShipmentId kumaşın girildiği sevkiyat; ipliği önce buradan düşer
     * @param fabricKg kumaş net kg
     * @param ratios kalitecinin yazdığı oranlar; toplamı tam 100
     * @param stock fasoncunun açık sevkiyatlarında kalan iplik
     * @return her iplik için düşüm ve yetmeyenler
     */
    public static Plan plan(
            UUID entryShipmentId, BigDecimal fabricKg, List<YarnRatio> ratios, List<YarnStockRow> stock) {
        requireValid(ratios);
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (int i = 0; i < ratios.size(); i++) {
            weights.put(Integer.toString(i), ratios.get(i).ratioPct());
        }
        Map<String, BigDecimal> kgByIndex = new LinkedHashMap<>();
        YarnDistribution.proportional(fabricKg, weights)
                .forEach(share -> kgByIndex.put(share.yarnType(), share.netKg()));
        Pool pool = new Pool();
        for (int i = 0; i < ratios.size(); i++) {
            BigDecimal kg = kgByIndex.get(Integer.toString(i));
            if (kg != null) {
                drawOne(candidates(entryShipmentId, ratios.get(i), stock), ratios.get(i), kg, pool);
            }
        }
        return new Plan(pool.yarns, pool.shortages);
    }

    /** Planın ilerleyen durumu: her satırdan kalan kilo ve şimdiye kadar çıkan sonuçlar. */
    private static final class Pool {
        private final Map<YarnStockRow, BigDecimal> left = new HashMap<>();
        private final List<FabricYarn> yarns = new ArrayList<>();
        private final List<YarnShortage> shortages = new ArrayList<>();
    }

    private static void drawOne(List<YarnStockRow> candidates, YarnRatio ratio, BigDecimal kg, Pool pool) {
        List<YarnDraw> draws = new ArrayList<>();
        BigDecimal remaining = kg;
        for (YarnStockRow row : candidates) {
            BigDecimal free = pool.left.getOrDefault(row, row.availableKg());
            BigDecimal take = free.min(remaining);
            if (take.signum() > 0) {
                draws.add(new YarnDraw(row.shipmentId(), row.shipmentNo(), row.rawYarnType(), row.lotId(), take));
                pool.left.put(row, free.subtract(take));
                remaining = remaining.subtract(take);
            }
        }
        String name =
                candidates.isEmpty() ? ratio.yarnType() : candidates.get(0).yarnName();
        String lotNo = candidates.isEmpty() ? null : candidates.get(0).lotNo();
        if (remaining.signum() > 0) {
            pool.shortages.add(new YarnShortage(name, ratio.lotId(), lotNo, kg, Kilograms.round(remaining)));
        }
        pool.yarns.add(new FabricYarn(name, ratio.lotId(), lotNo, ratio.ratioPct(), kg, draws));
    }

    /** Oranlar dolu, aynı iplik bir kez yazılmış ve toplamı tam 100 olmalı. */
    private static void requireValid(List<YarnRatio> ratios) {
        if (ratios.isEmpty()) {
            throw new InvalidLedgerInputException("yarns");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < ratios.size(); i++) {
            YarnRatio ratio = ratios.get(i);
            for (int j = 0; j < i; j++) {
                if (isSameYarn(ratios.get(j), ratio)) {
                    throw new InvalidLedgerInputException("yarns");
                }
            }
            total = total.add(ratio.ratioPct());
        }
        if (total.compareTo(YarnRatio.HUNDRED) != 0) {
            throw new InvalidLedgerInputException("yarns.ratioPct");
        }
    }

    private static boolean isSameYarn(YarnRatio left, YarnRatio right) {
        return YarnNames.same(left.yarnType(), right.yarnType()) && Objects.equals(left.lotId(), right.lotId());
    }

    /** İpliğin bulunduğu sevkiyatlar: önce kumaşın girildiği sevkiyat, sonra en eskiden yeniye. */
    private static List<YarnStockRow> candidates(UUID entryShipmentId, YarnRatio ratio, List<YarnStockRow> stock) {
        return stock.stream()
                .filter(row -> row.isSameYarn(ratio.yarnType(), ratio.lotId()))
                .filter(row -> row.availableKg().signum() > 0)
                .sorted(Comparator.comparing(
                                (YarnStockRow row) -> !row.shipmentId().equals(entryShipmentId))
                        .thenComparing(YarnStockRow::since)
                        .thenComparing(YarnStockRow::shipmentId))
                .toList();
    }
}
