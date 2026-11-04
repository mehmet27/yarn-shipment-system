package com.yarnshipment.reporting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

/**
 * Rapor dönemi; iki uç da dahil, Europe/Istanbul takvim günüdür.
 *
 * @param from ilk gün
 * @param to son gün
 */
public record ReportPeriod(LocalDate from, LocalDate to) {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    public static final int DEFAULT_DAYS = 30;
    public static final int MAX_DAYS = 366;

    public ReportPeriod {
        if (from.isAfter(to)) {
            throw new InvalidReportPeriodException("from is after to");
        }
        if (from.plusDays(MAX_DAYS).isBefore(to)) {
            throw new InvalidReportPeriodException("period longer than " + MAX_DAYS + " days");
        }
    }

    /** Boş uçlar için varsayılan: bitiş bugün, başlangıç bitişten {@link #DEFAULT_DAYS} gün önce. */
    public static ReportPeriod of(@Nullable LocalDate from, @Nullable LocalDate to, LocalDate today) {
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        return new ReportPeriod(start, end);
    }

    /** Dönemin ilk anı (dahil). */
    public Instant start() {
        return from.atStartOfDay(ZONE).toInstant();
    }

    /** Dönemin bitiş anı (hariç): son günün ertesi gün başlangıcı. */
    public Instant endExclusive() {
        return to.plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}
