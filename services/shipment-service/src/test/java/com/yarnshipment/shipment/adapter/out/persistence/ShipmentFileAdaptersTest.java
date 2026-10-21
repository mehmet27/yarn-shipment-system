package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Geçmiş ve çuval okuması, yerel örnek veri üzerinde gerçek PostgreSQL'e karşı. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ShipmentHistoryAdapter.class, ShipmentBagsAdapter.class})
class ShipmentFileAdaptersTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    /** Örnek veride kontrol aşamasındaki (CHECKING) sevkiyat. */
    private static final UUID CHECKING = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0006");

    private static final UUID DRAFT = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0001");
    private static final int BAGS_PER_SHIPMENT = 5;
    private static final int MATCHED_SCANS = 4;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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
        List<StatusChange> changes = history.findHistory(CHECKING);

        assertThat(changes)
                .extracting(StatusChange::to)
                .containsExactly(
                        ShipmentStatus.DRAFT,
                        ShipmentStatus.LOADED,
                        ShipmentStatus.IN_TRANSIT,
                        ShipmentStatus.AWAITING_DELIVERY,
                        ShipmentStatus.CHECKING);
        assertThat(changes.getFirst().from()).isNull();
        assertThat(changes.get(1).from()).isEqualTo(ShipmentStatus.DRAFT);
    }

    @Test
    @DisplayName("Çıkış çuvalları atanmış olanlardır; taslakta yoktur")
    void shouldReadDispatchBags() {
        List<DispatchBag> assigned = bags.findDispatchBags(CHECKING);

        assertThat(assigned)
                .hasSize(BAGS_PER_SHIPMENT)
                .extracting(DispatchBag::uniqueKey)
                .first()
                .isEqualTo("BAG-6-1");
        assertThat(bags.findDispatchBags(DRAFT)).isEmpty();
    }

    @Test
    @DisplayName("Teslim okutmaları dört eşleşen ve bir eşleşmeyen anahtarla gelir; teslimi olmayanda boştur")
    void shouldReadDeliveryScans() {
        List<DeliveryScan> scans = bags.findDeliveryScans(CHECKING);

        assertThat(scans).hasSize(BAGS_PER_SHIPMENT);
        assertThat(scans.stream().filter(DeliveryScan::matched)).hasSize(MATCHED_SCANS);
        assertThat(bags.findDeliveryScans(DRAFT)).isEmpty();
    }
}
