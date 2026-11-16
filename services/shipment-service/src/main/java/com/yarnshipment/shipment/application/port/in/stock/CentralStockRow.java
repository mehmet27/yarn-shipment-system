package com.yarnshipment.shipment.application.port.in.stock;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Merkez stokta bir iplik tanımının eldeki kilosu.
 *
 * @param productId iplik tanımı; tanımla eşlenemeyen eski satırlarda boş
 * @param yarnType iplik adı (tanımın güncel adı)
 * @param stockKg hareketlerin toplamı; eksiye düşmüşse giriş eksiktir
 */
public record CentralStockRow(@Nullable UUID productId, String yarnType, BigDecimal stockKg) {}
