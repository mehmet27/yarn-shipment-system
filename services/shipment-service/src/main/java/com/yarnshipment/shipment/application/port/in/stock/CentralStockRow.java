package com.yarnshipment.shipment.application.port.in.stock;

import java.math.BigDecimal;

/**
 * Merkez stokta bir iplik cinsinin eldeki kilosu.
 *
 * @param yarnType iplik cinsi
 * @param stockKg hareketlerin toplamı; eksiye düşmüşse giriş eksiktir
 */
public record CentralStockRow(String yarnType, BigDecimal stockKg) {}
