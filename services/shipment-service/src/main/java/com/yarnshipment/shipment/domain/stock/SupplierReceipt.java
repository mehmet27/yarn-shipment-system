package com.yarnshipment.shipment.domain.stock;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir tedarikçiden bir iplik cinsinden bugüne kadar gelen iplik.
 *
 * @param supplierId tedarikçi tanımı; kapıdan aktarmada tanımla eşleşmeyen tedarikçide boş
 * @param supplierName tedarikçinin adı
 * @param productId iplik tanımı; eşlenemeyen eski satırda boş
 * @param yarnType iplik cinsi
 * @param receivedKg gelen toplam kilo
 */
public record SupplierReceipt(
        @Nullable UUID supplierId,
        String supplierName,
        @Nullable UUID productId,
        String yarnType,
        BigDecimal receivedKg) {}
