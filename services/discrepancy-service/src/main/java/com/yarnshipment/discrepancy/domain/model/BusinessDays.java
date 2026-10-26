package com.yarnshipment.discrepancy.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** İş günü hesabı: cumartesi ve pazar sayılmaz. Resmî tatiller sonraki adımda takvim tablosundan gelir. */
public final class BusinessDays {

    /** Uyuşmazlığın hedef kapanış süresi (ön analiz, Bölüm 12.2). */
    public static final int DISCREPANCY_DUE_DAYS = 5;

    private BusinessDays() {}

    public static Instant plus(Instant from, int businessDays, ZoneId zone) {
        ZonedDateTime cursor = from.atZone(zone);
        int added = 0;
        while (added < businessDays) {
            cursor = cursor.plusDays(1);
            if (!isWeekend(cursor.getDayOfWeek())) {
                added++;
            }
        }
        return cursor.toInstant();
    }

    private static boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
