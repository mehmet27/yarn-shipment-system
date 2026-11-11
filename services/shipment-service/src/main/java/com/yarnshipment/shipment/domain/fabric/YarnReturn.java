package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Dönen iplik ya da fasoncudan gelen iplik kaydı; yalnız eklenir.
 *
 * @param id kayıt
 * @param shipmentId ilgili sevkiyat
 * @param subcontractorId fasoncu
 * @param direction yön
 * @param yarnType iplik cinsi
 * @param lotId lot, isteğe bağlı
 * @param netKg net kg, sıfırdan büyük
 * @param waybillNo irsaliye numarası, isteğe bağlı
 * @param createdAt sunucunun kaydı aldığı zaman
 * @param createdBy kaydeden kullanıcı
 */
public record YarnReturn(
        UUID id,
        UUID shipmentId,
        UUID subcontractorId,
        ReturnDirection direction,
        String yarnType,
        @Nullable UUID lotId,
        BigDecimal netKg,
        @Nullable String waybillNo,
        Instant createdAt,
        UUID createdBy) {

    public YarnReturn {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(createdBy, "createdBy");
        yarnType = yarnType.strip();
        if (yarnType.isEmpty()) {
            throw new InvalidLedgerInputException("yarnType");
        }
        netKg = Kilograms.round(netKg);
        if (netKg.signum() <= 0) {
            throw new InvalidLedgerInputException("netKg");
        }
        waybillNo = waybillNo == null || waybillNo.isBlank() ? null : waybillNo.strip();
    }
}
