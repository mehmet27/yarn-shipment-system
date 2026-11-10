package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** İplik cinsi × fasoncu bakiyesi; {@code balanceKg} hareketlerin işaretli toplamıdır. */
public record BalanceMatrixRow(
        String yarnType,
        UUID subcontractorId,
        String subcontractorName,
        BigDecimal balanceKg,
        BigDecimal inTransitKg) {}
