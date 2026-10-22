package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Çuval atama yazması, örnek taslak sevkiyat üzerinde gerçek PostgreSQL'e karşı. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BagAssignmentAdapter.class, ShipmentBagsAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class BagAssignmentAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID DRAFT = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0001");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BagAssignmentAdapter assignments;

    @Autowired
    private ShipmentBagsAdapter bags;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Çuvallar yazılır, okunur ve tekrar gönderilince yeniden yazılmaz")
    void shouldAssignOnceAndSkipRepeats() {
        BagAssignment assignment = new BagAssignment(
                DRAFT,
                List.of(
                        new ReservedBag(UUID.randomUUID(), "BAG-0001", "P-2026-0001", "A"),
                        new ReservedBag(UUID.randomUUID(), "BAG-0002", "P-2026-0001", "A")),
                ScanMethod.LIST);

        int first = assignments.assign(assignment, ACTOR, NOW);
        int repeat = assignments.assign(assignment, ACTOR, NOW);

        assertThat(first).isEqualTo(2);
        assertThat(repeat).isZero();
        assertThat(bags.findDispatchBags(DRAFT))
                .extracting(DispatchBag::uniqueKey)
                .contains("BAG-0001", "BAG-0002");
    }
}
