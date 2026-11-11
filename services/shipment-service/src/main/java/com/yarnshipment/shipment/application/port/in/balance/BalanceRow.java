package com.yarnshipment.shipment.application.port.in.balance;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fasoncunun bir iplik cinsindeki bakiyesi.
 *
 * @param subcontractorId fasoncu
 * @param subcontractorTitle fasoncu unvanı
 * @param yarnType iplik cinsi
 * @param balanceKg hareketlerin toplamı (KR-13)
 * @param inTransitKg yolda olan, henüz bakiyeye girmemiş kilo (KR-12)
 */
public record BalanceRow(
        UUID subcontractorId,
        String subcontractorTitle,
        String yarnType,
        BigDecimal balanceKg,
        BigDecimal inTransitKg) {}
