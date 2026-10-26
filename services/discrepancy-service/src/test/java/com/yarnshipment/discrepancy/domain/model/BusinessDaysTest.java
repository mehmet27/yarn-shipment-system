package com.yarnshipment.discrepancy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessDaysTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    @Test
    @DisplayName("Hafta sonu sayılmaz: cuma gününe beş iş günü eklenince sonraki cuma bulunur")
    void shouldSkipWeekends() {
        Instant friday = Instant.parse("2026-10-23T09:00:00Z");

        assertThat(BusinessDays.plus(friday, BusinessDays.DISCREPANCY_DUE_DAYS, ISTANBUL))
                .isEqualTo(Instant.parse("2026-10-30T09:00:00Z"));
    }

    @Test
    @DisplayName("Cumartesi başlayan süre pazartesiden sayılır")
    void shouldStartCountingAfterWeekend() {
        Instant saturday = Instant.parse("2026-10-24T09:00:00Z");

        assertThat(BusinessDays.plus(saturday, 1, ISTANBUL)).isEqualTo(Instant.parse("2026-10-26T09:00:00Z"));
    }
}
