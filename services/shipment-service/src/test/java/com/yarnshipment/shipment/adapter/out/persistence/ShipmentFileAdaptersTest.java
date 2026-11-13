package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Geçmiş ve çuval okuması, yerel örnek veri üzerinde gerçek PostgreSQL'e karşı. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ShipmentHistoryAdapter.class, ShipmentBagsAdapter.class})
class ShipmentFileAdaptersTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    /** Örnek veride tamamlanmış (COMPLETED) sevkiyat. */
    private static final UUID COMPLETED = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0006");

    private static final UUID DRAFT = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0001");
    private static final int BAGS_PER_SHIPMENT = 5;

    @Autowired
    private ShipmentHistoryAdapter history;

    @Autowired
    private ShipmentBagsAdapter bags;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Geçmiş eskiden yeniye gelir; ilk satırın önceki durumu yoktur")
    void shouldReadHistoryOldestFirst() {
        List<StatusChange> changes = history.findHistory(COMPLETED);

        assertThat(changes)
                .extracting(StatusChange::to)
                .containsExactly(ShipmentStatus.DRAFT, ShipmentStatus.AWAITING_DELIVERY, ShipmentStatus.COMPLETED);
        assertThat(changes.getFirst().from()).isNull();
        assertThat(changes.get(1).from()).isEqualTo(ShipmentStatus.DRAFT);
    }

    @Test
    @DisplayName("Çıkış çuvalları sıra numarasıyla gelir; taslakta yoktur")
    void shouldReadDispatchBags() {
        List<DispatchBag> assigned = bags.findDispatchBags(COMPLETED);

        assertThat(assigned)
                .hasSize(BAGS_PER_SHIPMENT)
                .extracting(DispatchBag::seqNo)
                .containsExactlyElementsOf(
                        IntStream.rangeClosed(1, BAGS_PER_SHIPMENT).boxed().toList());
        assertThat(bags.findDispatchBags(DRAFT)).isEmpty();
    }
}
