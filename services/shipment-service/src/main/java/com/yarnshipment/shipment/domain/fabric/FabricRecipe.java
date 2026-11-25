package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kumaş cinsinin varsayılan iplik karışımı (reçete). Kumaş girerken oranlar hazır dolu gelir, kaliteci düzeltebilir.
 * Reçete iplik cinsi düzeyindedir; fasoncuda aynı cinsin birden çok partisi varsa oran ekranda partilere elde bulunan
 * kiloya göre bölünür.
 *
 * @param id kayıt
 * @param fabricType kumaş cinsi
 * @param yarns karışım; oranların toplamı 100
 * @param isActive pasif reçete kumaş girerken önerilmez
 */
public record FabricRecipe(UUID id, String fabricType, List<Line> yarns, boolean isActive) {

    public static final int MAX_TEXT_LENGTH = 128;

    public FabricRecipe {
        fabricType = fabricType.strip();
        if (fabricType.isEmpty() || fabricType.length() > MAX_TEXT_LENGTH) {
            throw new InvalidLedgerInputException("fabricType");
        }
        yarns = List.copyOf(yarns);
        if (yarns.isEmpty()) {
            throw new InvalidLedgerInputException("yarns");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < yarns.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (YarnNames.same(yarns.get(j).yarnType(), yarns.get(i).yarnType())) {
                    throw new InvalidLedgerInputException("yarns");
                }
            }
            total = total.add(yarns.get(i).ratioPct());
        }
        if (total.compareTo(YarnRatio.HUNDRED) != 0) {
            throw new InvalidLedgerInputException("yarns.ratioPct");
        }
    }

    /**
     * Reçetenin bir ipliği.
     *
     * @param yarnType iplik cinsinin adı
     * @param ratioPct yüzde, en çok iki ondalık
     */
    public record Line(String yarnType, BigDecimal ratioPct) {

        public Line {
            yarnType = yarnType.strip();
            if (yarnType.isEmpty()) {
                throw new InvalidLedgerInputException("yarns.yarnType");
            }
            // Oran kuralları kumaş girişindekiyle aynıdır.
            ratioPct = new YarnRatio(yarnType, null, ratioPct).ratioPct();
        }
    }
}
