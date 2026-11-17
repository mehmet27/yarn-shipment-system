package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bir fasoncudaki kalan iplik; değer her iplik cinsinin o fasoncuya son girilen fiyatıyla hesaplanır.
 *
 * @param balanceKg kalan iplik (eksi bakiye sıfır sayılır)
 * @param amount fiyatı bilinen iplikten hesaplanan tutar
 * @param unpricedKg fiyatı bilinmeyen kalan iplik
 */
public record SubcontractorAmount(
        UUID subcontractorId,
        String subcontractorName,
        BigDecimal balanceKg,
        BigDecimal amount,
        BigDecimal unpricedKg) {}
