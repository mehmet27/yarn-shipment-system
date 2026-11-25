package com.yarnshipment.shipment.domain.fabric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fasoncunun açık bir sevkiyatında kalan iplik: sevkiyatta gelen, ama kumaşa girmemiş, dönmemiş ve faturalanmamış kilo.
 *
 * @param shipmentId sevkiyat
 * @param shipmentNo sevkiyat numarası
 * @param since sevkiyatın tamamlandığı zaman; düşüm en eski sevkiyattan başlar
 * @param rawYarnType defterde yazılı iplik anahtarı (tanım kimliği ya da metin)
 * @param yarnName ekranda görünen iplik adı
 * @param lotId iplik partisi
 * @param lotNo parti numarası
 * @param supplierName tedarikçi
 * @param availableKg kalan kilo, sıfırdan büyük
 */
public record YarnStockRow(
        UUID shipmentId,
        String shipmentNo,
        Instant since,
        String rawYarnType,
        String yarnName,
        @Nullable UUID lotId,
        @Nullable String lotNo,
        @Nullable String supplierName,
        BigDecimal availableKg) {

    /** Aynı iplik ve parti mi (ad harf farkına bakılmaz). */
    public boolean isSameYarn(String name, @Nullable UUID lot) {
        return YarnNames.same(yarnName, name) && java.util.Objects.equals(lotId, lot);
    }
}
