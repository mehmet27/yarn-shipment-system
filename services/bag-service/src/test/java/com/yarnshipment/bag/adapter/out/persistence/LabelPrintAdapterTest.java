package com.yarnshipment.bag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagLabel;
import com.yarnshipment.bag.domain.model.LabeledBag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Etiket baskı kayıtları, gerçek PostgreSQL'de ve yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    BagQueryAdapter.class,
    LabelPrintAdapter.class,
    UuidV7Generator.class
})
class LabelPrintAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID LOT_A = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final int LIMIT = 500;
    private static final long MINUTE = 60;
    private static final int SECOND_PRINT = 2;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BagQueryAdapter bags;

    @Autowired
    private LabelPrintAdapter prints;

    @Autowired
    private JdbcClient jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Baskılar sayılır, kaydedilir ve partinin çuval listesinde baskı sayısıyla görünür")
    void shouldRecordAndCountPrints() {
        Bag bag = bags.findByUniqueKey("BAG-0002").orElseThrow();
        assertThat(prints.lockAndCountPrints(List.of(bag.id()))).isEmpty();
        assertThat(prints.lockAndCountPrints(List.of())).isEmpty();

        prints.record(List.of(new BagLabel(bag, 1)), "yok sayılır", ACTOR, NOW);
        prints.record(List.of(new BagLabel(bag, SECOND_PRINT)), "etiket yırtıldı", ACTOR, NOW.plusSeconds(MINUTE));
        prints.record(List.of(), null, ACTOR, NOW);

        assertThat(prints.lockAndCountPrints(List.of(bag.id()))).isEqualTo(Map.of(bag.id(), SECOND_PRINT));
        List<String> reasons = jdbc.sql(
                        "SELECT coalesce(reprint_reason, '-') FROM label_print WHERE bag_id = :id ORDER BY print_no")
                .param("id", bag.id())
                .query((rs, rowNumber) -> rs.getString(1))
                .list();
        assertThat(reasons).containsExactly("-", "etiket yırtıldı");
        LabeledBag labeled = bags.findByLot(LOT_A, LIMIT).stream()
                .filter(candidate -> candidate.bag().id().equals(bag.id()))
                .findFirst()
                .orElseThrow();
        assertThat(labeled.printCount()).isEqualTo(SECOND_PRINT);
        assertThat(labeled.lastPrintedAt()).isEqualTo(NOW.plusSeconds(MINUTE));
        assertThat(bags.findByLot(LOT_A, LIMIT))
                .filteredOn(candidate -> candidate.printCount() == 0)
                .allSatisfy(candidate -> assertThat(candidate.lastPrintedAt()).isNull());
        assertThat(bags.findByLot(LOT_A, 1)).hasSize(1);
    }
}
