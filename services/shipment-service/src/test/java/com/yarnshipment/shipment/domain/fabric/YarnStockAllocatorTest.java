package com.yarnshipment.shipment.domain.fabric;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.fabric.YarnStockAllocator.Bucket;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class YarnStockAllocatorTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final UUID LOT_OLD = UUID.fromString("0192f0a0-0000-7000-8000-0000000000f1");
    private static final UUID LOT_NEW = UUID.fromString("0192f0a0-0000-7000-8000-0000000000f2");

    private static Bucket bucket(@Nullable UUID lot, String kg, String firstAt) {
        return new Bucket(
                SHIPMENT,
                "SV-1",
                Instant.parse("2026-11-01T00:00:00Z"),
                "A",
                "A",
                lot,
                null,
                null,
                new BigDecimal(kg),
                Instant.parse(firstAt));
    }

    @Test
    @DisplayName("Partili sevkiyatta kalan kilo partiye göre listelenir")
    void shouldListRemainingPerLot() {
        List<YarnStockRow> rows = YarnStockAllocator.allocate(
                List.of(bucket(LOT_OLD, "100", "2026-11-01T00:00:00Z"), bucket(LOT_NEW, "50", "2026-11-01T00:00:01Z")));

        assertThat(rows)
                .extracting(YarnStockRow::availableKg)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("100"), new BigDecimal("50"));
    }

    @Test
    @DisplayName("Partisiz düşen kilo (dönen iplik, fatura) en eski partiden başlayarak düşer")
    void shouldDeductUnlottedConsumptionFromOldestLot() {
        List<YarnStockRow> rows = YarnStockAllocator.allocate(List.of(
                bucket(LOT_NEW, "50", "2026-11-01T00:00:01Z"),
                bucket(LOT_OLD, "100", "2026-11-01T00:00:00Z"),
                bucket(null, "-120", "2026-11-02T00:00:00Z")));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.lotId()).isEqualTo(LOT_NEW);
            assertThat(row.availableKg()).isEqualByComparingTo("30");
        });
    }

    @Test
    @DisplayName("Tükenen ya da eksiye düşen satır listelenmez")
    void shouldDropEmptyRows() {
        List<YarnStockRow> rows = YarnStockAllocator.allocate(
                List.of(bucket(LOT_OLD, "0", "2026-11-01T00:00:00Z"), bucket(null, "-5", "2026-11-02T00:00:00Z")));

        assertThat(rows).isEmpty();
    }
}
