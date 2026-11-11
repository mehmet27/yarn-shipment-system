package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyata gelen kumaş (analiz 5.8). Tükettiği iplik cinslerinin toplamı kumaş net kg'sine eşittir; kayıt yalnız
 * eklenir.
 *
 * @param id kayıt
 * @param shipmentId sevkiyat
 * @param subcontractorId fasoncu
 * @param fabricType kumaş cinsi
 * @param netKg kumaş net kg
 * @param rollCount top/rulo sayısı, isteğe bağlı
 * @param waybillNo kumaş irsaliye numarası
 * @param receivedAt kumaşın geldiği zaman
 * @param yarns tükettiği iplikler
 * @param createdBy kaydeden kullanıcı
 */
public record FabricReceipt(
        UUID id,
        UUID shipmentId,
        UUID subcontractorId,
        String fabricType,
        BigDecimal netKg,
        @Nullable Integer rollCount,
        String waybillNo,
        Instant receivedAt,
        List<YarnShare> yarns,
        UUID createdBy) {

    public static final int MAX_TEXT_LENGTH = 128;

    public FabricReceipt {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(createdBy, "createdBy");
        fabricType = requiredText(fabricType, "fabricType");
        waybillNo = requiredText(waybillNo, "waybillNo");
        netKg = Kilograms.round(netKg);
        if (netKg.signum() <= 0) {
            throw new InvalidLedgerInputException("netKg");
        }
        if (rollCount != null && rollCount <= 0) {
            throw new InvalidLedgerInputException("rollCount");
        }
        yarns = List.copyOf(yarns);
        BigDecimal yarnTotal = yarns.stream().map(YarnShare::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (yarns.isEmpty() || yarnTotal.compareTo(netKg) != 0) {
            throw new InvalidLedgerInputException("yarns");
        }
    }

    private static String requiredText(String value, String field) {
        String text = value.strip();
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidLedgerInputException(field);
        }
        return text;
    }
}
