package com.yarnshipment.shipment.application.port.in.fabric;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fasoncunun elinde, kumaşa girebilecek bir iplik satırı. Aynı cinsin tedarikçisi ya da partisi farklıysa ayrı
 * satırdır.
 *
 * @param yarnType iplik cinsinin adı
 * @param lotId iplik partisi
 * @param lotNo parti numarası
 * @param supplierName tedarikçi
 * @param totalKg fasoncunun açık sevkiyatlarındaki toplam kalan kilo
 * @param ownKg kumaşın girildiği sevkiyattaki kalan kilo
 */
public record FabricYarnOption(
        String yarnType,
        @Nullable UUID lotId,
        @Nullable String lotNo,
        @Nullable String supplierName,
        BigDecimal totalKg,
        BigDecimal ownKg) {}
