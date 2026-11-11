package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Açılış bakiyesi satırı (Excel'den). */
public record OpeningBalanceRow(UUID subcontractorId, String subcontractorName, String yarnType, BigDecimal netKg) {}
