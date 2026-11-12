package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
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
@Import({
    RecordingDomainEventPublisher.class,
    BagAssignmentAdapter.class,
    ShipmentBagsAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class BagAssignmentAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID DRAFT = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0001");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final int TOTAL_BAGS = 3;
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
    @DisplayName("Çuvallar sıra numarası ve kiloyla yazılır; sonraki ekleme son numaradan devam eder")
    void shouldAssignWithSequenceNumbers() {
        BagAssignment first = new BagAssignment(DRAFT, List.of(new BigDecimal("25.000"), new BigDecimal("24.500")));
        BagAssignment second = new BagAssignment(DRAFT, List.of(new BigDecimal("26.250")));

        assertThat(assignments.assign(first, ACTOR, NOW)).isEqualTo(2);
        assertThat(assignments.assign(second, ACTOR, NOW)).isEqualTo(1);

        assertThat(bags.findDispatchBags(DRAFT))
                .extracting(DispatchBag::seqNo)
                .containsExactlyElementsOf(
                        IntStream.rangeClosed(1, TOTAL_BAGS).boxed().toList());
        assertThat(bags.findDispatchBags(DRAFT))
                .extracting(DispatchBag::weightKg)
                .containsExactly(new BigDecimal("25.000"), new BigDecimal("24.500"), new BigDecimal("26.250"));
    }
}
