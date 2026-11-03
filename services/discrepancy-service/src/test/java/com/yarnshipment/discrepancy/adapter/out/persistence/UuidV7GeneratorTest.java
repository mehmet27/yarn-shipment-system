package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

    private static final int COUNT = 10_000;
    private static final int UUID_VERSION = 7;

    @Test
    @DisplayName("aynı milisaniyede üretilen kimlikler üretim sırasıyla artar")
    void shouldStayMonotonicWithinSameMillisecond() {
        Clock frozen = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);
        UuidV7Generator ids = new UuidV7Generator(frozen);

        List<UUID> generated = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            generated.add(ids.next());
        }

        assertThat(generated).isSortedAccordingTo(Comparator.comparing(UuidV7GeneratorTest::unsigned));
        assertThat(generated).doesNotHaveDuplicates();
        assertThat(generated).allSatisfy(id -> assertThat(id.version()).isEqualTo(UUID_VERSION));
    }

    /** PostgreSQL uuid'leri bayt bayt (işaretsiz) karşılaştırır; Java'nın UUID.compareTo'su işaretlidir. */
    private static String unsigned(UUID id) {
        return id.toString();
    }
}
