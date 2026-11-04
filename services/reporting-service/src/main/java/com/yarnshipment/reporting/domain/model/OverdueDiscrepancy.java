package com.yarnshipment.reporting.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Vadesi geçmiş açık uyuşmazlık.
 *
 * @param discrepancyId uyuşmazlık
 * @param discrepancyNo okunabilir numara
 * @param shipmentId sevkiyat
 * @param severity {@code YELLOW} ya da {@code RED}
 * @param dueAt vade
 */
public record OverdueDiscrepancy(
        UUID discrepancyId, String discrepancyNo, UUID shipmentId, String severity, Instant dueAt) {}
