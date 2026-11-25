package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kumaş kilosunu sevkiyatın reçete oranlarıyla iplik cinslerine dağıtır (analiz 5.8). Dağıtım gram (0,001 kg)
 * hassasiyetindedir ve toplamı her zaman kumaş kilosuna eşittir: artan gramlar en büyük kesirli paydan başlayarak
 * verilir.
 */
public final class YarnDistribution {

    private YarnDistribution() {}

    /**
     * Kumaş kilosunu reçete oranına göre paylaştırır.
     *
     * @param fabricKg dağıtılacak kumaş net kg
     * @param recipeKg iplik cinsine göre sevkiyatta gönderilen kg (oran kaynağı), sıra korunur
     */
    public static List<YarnShare> proportional(BigDecimal fabricKg, Map<String, BigDecimal> recipeKg) {
        BigInteger totalGrams = grams(fabricKg);
        BigDecimal recipeTotal = recipeKg.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (recipeKg.isEmpty() || recipeTotal.signum() <= 0) {
            throw new InvalidLedgerInputException("yarns");
        }
        List<Portion> portions = new ArrayList<>();
        BigInteger assigned = BigInteger.ZERO;
        for (Map.Entry<String, BigDecimal> entry : recipeKg.entrySet()) {
            BigInteger[] division = totalGrams.multiply(grams(entry.getValue())).divideAndRemainder(grams(recipeTotal));
            portions.add(new Portion(entry.getKey(), division[0], division[1]));
            assigned = assigned.add(division[0]);
        }
        int leftover = totalGrams.subtract(assigned).intValueExact();
        List<Portion> byRemainder = new ArrayList<>(portions);
        byRemainder.sort(Comparator.comparing(Portion::remainder).reversed());
        for (int i = 0; i < leftover; i++) {
            byRemainder.get(i).grams = byRemainder.get(i).grams.add(BigInteger.ONE);
        }
        return portions.stream()
                .filter(portion -> portion.grams.signum() > 0)
                .map(portion -> new YarnShare(portion.yarnType, new BigDecimal(portion.grams, Kilograms.SCALE)))
                .toList();
    }

    /**
     * Açıkça verilen iplik paylarını sevkiyatta gönderilen cinslerle eşler ve aynı cinsi birleştirir. Boşluk ve
     * büyük/küçük harf (İ/ı dahil) önemsizdir; ad, gönderilen cinsin yazılışına çevrilir. Sevkiyatın cins bilgisi yoksa
     * (eski kayıt) adlar denetlenmez.
     *
     * @param given istekteki paylar
     * @param dispatchedKg iplik cinsine göre sevkiyatta gönderilen kg
     * @param shipmentId sevkiyat (hata iletisi için)
     * @throws FabricYarnNotInShipmentException cins sevkiyatta gönderilenler arasında yoksa
     */
    public static List<YarnShare> explicit(
            List<YarnShare> given, Map<String, BigDecimal> dispatchedKg, UUID shipmentId) {
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        given.forEach(share -> merged.merge(
                canonicalName(dispatchedKg, shipmentId, share.yarnType()), share.netKg(), BigDecimal::add));
        return merged.entrySet().stream()
                .map(entry -> new YarnShare(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static String canonicalName(Map<String, BigDecimal> dispatchedKg, UUID shipmentId, String yarnType) {
        if (dispatchedKg.isEmpty()) {
            return yarnType;
        }
        return dispatchedKg.keySet().stream()
                .filter(name -> sameName(name, yarnType))
                .findFirst()
                .orElseThrow(() -> new FabricYarnNotInShipmentException(shipmentId, yarnType));
    }

    private static boolean sameName(String dispatchedName, String given) {
        return YarnNames.same(dispatchedName, given);
    }

    private static BigInteger grams(BigDecimal kg) {
        return Kilograms.round(kg).unscaledValue();
    }

    private static final class Portion {
        private final String yarnType;
        private BigInteger grams;
        private final BigInteger remainder;

        private Portion(String yarnType, BigInteger grams, BigInteger remainder) {
            this.yarnType = yarnType;
            this.grams = grams;
            this.remainder = remainder;
        }

        private BigInteger remainder() {
            return remainder;
        }
    }
}
