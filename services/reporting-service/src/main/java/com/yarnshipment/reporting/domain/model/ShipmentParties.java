package com.yarnshipment.reporting.domain.model;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Okuma modelindeki sevkiyatın tarafları.
 *
 * @param siteId lokasyon
 * @param driverId şoför; henüz yola çıkmadıysa boş
 * @param subcontractorId fasoncu
 */
public record ShipmentParties(UUID siteId, @Nullable UUID driverId, UUID subcontractorId) {}
