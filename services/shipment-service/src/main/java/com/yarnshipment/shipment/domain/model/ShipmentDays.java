package com.yarnshipment.shipment.domain.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Sevkiyat günü Türkiye saatine göre hesaplanır (fabrikanın günü); sunucu saat dilimine bağlı değildir. */
public final class ShipmentDays {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private ShipmentDays() {}

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
