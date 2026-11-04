package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Rapor sorgusu; boş alanlar varsayılan değeri kullanır.
 *
 * @param actor isteyen
 * @param from dönem başı
 * @param to dönem sonu
 * @param siteId yalnızca bu lokasyon
 */
public record ReportQuery(
        Actor actor,
        @Nullable LocalDate from,
        @Nullable LocalDate to,
        @Nullable UUID siteId) {}
