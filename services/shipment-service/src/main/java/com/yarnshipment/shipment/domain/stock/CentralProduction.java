package com.yarnshipment.shipment.domain.stock;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Merkez fabrikanın ürettiği kumaş. Kumaşın iplik payları toplamı kumaş net kg'sine eşittir; stoktan düşen iplik
 * fire ile birlikte kumaş / (1 - fire) kadardır (fasoncu fire hesabıyla aynı formül).
 *
 * @param id kayıt
 * @param fabricType kumaş cinsi
 * @param netKg kumaş net kg
 * @param fireRate fire oranı, 0 ile 0,20 arası
 * @param rollCount top/rulo sayısı, isteğe bağlı
 * @param waybillNo iç irsaliye/üretim numarası, isteğe bağlı
 * @param producedAt üretim zamanı
 * @param yarns kumaşın yapıldığı iplikler
 * @param createdBy kaydeden kullanıcı
 */
public record CentralProduction(
        UUID id,
        String fabricType,
        BigDecimal netKg,
        BigDecimal fireRate,
        @Nullable Integer rollCount,
        @Nullable String waybillNo,
        Instant producedAt,
        List<YarnShare> yarns,
        UUID createdBy) {

    public static final int MAX_TEXT_LENGTH = 128;
    private static final BigDecimal MAX_FIRE_RATE = new BigDecimal("0.20");

    public CentralProduction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(producedAt, "producedAt");
        Objects.requireNonNull(createdBy, "createdBy");
        fabricType = fabricType.strip();
        if (fabricType.isEmpty() || fabricType.length() > MAX_TEXT_LENGTH) {
            throw new InvalidLedgerInputException("fabricType");
        }
        netKg = Kilograms.round(netKg);
        if (netKg.signum() <= 0) {
            throw new InvalidLedgerInputException("netKg");
        }
        if (fireRate.signum() < 0 || fireRate.compareTo(MAX_FIRE_RATE) > 0) {
            throw new InvalidLedgerInputException("fireRate");
        }
        if (rollCount != null && rollCount <= 0) {
            throw new InvalidLedgerInputException("rollCount");
        }
        if (waybillNo != null && (waybillNo.isBlank() || waybillNo.length() > MAX_TEXT_LENGTH)) {
            throw new InvalidLedgerInputException("waybillNo");
        }
        yarns = List.copyOf(yarns);
        BigDecimal total = yarns.stream().map(YarnShare::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (yarns.isEmpty() || total.compareTo(netKg) != 0) {
            throw new InvalidLedgerInputException("yarns");
        }
    }

    /** Her iplik payı için stoktan düşen (eksi işaretli, fire dahil) hareketler. */
    public List<CentralStockMovement> consumptions() {
        BigDecimal keep = BigDecimal.ONE.subtract(fireRate);
        return yarns.stream()
                .map(share -> new CentralStockMovement(
                        UUID.randomUUID(),
                        share.yarnType(),
                        CentralStockKind.PRODUCTION,
                        share.netKg()
                                .divide(keep, Kilograms.SCALE, RoundingMode.HALF_UP)
                                .negate(),
                        id,
                        share.netKg(),
                        null,
                        producedAt,
                        createdBy))
                .toList();
    }
}
