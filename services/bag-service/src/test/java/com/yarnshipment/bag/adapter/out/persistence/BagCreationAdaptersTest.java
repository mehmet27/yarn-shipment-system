package com.yarnshipment.bag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.bag.application.port.out.BagCreationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.bag.domain.model.PackageType;
import java.time.Instant;
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

/** Çuval üretimi ve parti listesi, gerçek PostgreSQL'de ve yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    BagCreationAdapter.class,
    BagQueryAdapter.class,
    LotQueryAdapter.class,
    UniqueKeyGenerator.class,
    UuidV7Generator.class
})
class BagCreationAdaptersTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID SITE_A = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID LOT_A = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final int SEEDED_BAGS_OF_LOT = 4;
    private static final int NEW_BAGS = 3;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BagCreationAdapter creations;

    @Autowired
    private BagQueryAdapter bags;

    @Autowired
    private LotQueryAdapter lots;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Kilitli parti çuval sayısıyla okunur; lokasyon listesi yalnızca o lokasyonun partilerini döner")
    void shouldReadLockedLots() {
        LockedLot lot = lots.findLocked(LOT_A).orElseThrow();

        assertThat(lot.lotNo()).isEqualTo("P-2026-0001");
        assertThat(lot.qualityGradeCode()).isEqualTo("A");
        assertThat(lot.bagCount()).isEqualTo(SEEDED_BAGS_OF_LOT);
        assertThat(lots.findLocked(UUID.randomUUID())).isEmpty();
        assertThat(lots.findLockedInSites(List.of(SITE_A))).extracting(LockedLot::lotId).containsExactly(LOT_A);
        assertThat(lots.findLockedInSites(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Üretilen çuvallar tekil, tahmin edilemeyen anahtar alır; parti ve kalite partiden gelir")
    void shouldCreateBagsWithGeneratedKeys() {
        LockedLot lot = lots.findLocked(LOT_A).orElseThrow();

        List<Bag> created = creations.create(new BagCreationPort.NewBags(lot, PackageType.BOX, NEW_BAGS), ACTOR, NOW);

        assertThat(created).hasSize(NEW_BAGS).allSatisfy(bag -> {
            assertThat(bag.uniqueKey()).matches("BAG-[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}");
            assertThat(bag.status()).isEqualTo(BagStatus.CREATED);
            assertThat(bag.qualityGradeCode()).isEqualTo("A");
            assertThat(bag.lotNo()).isEqualTo("P-2026-0001");
        });
        assertThat(created).extracting(Bag::uniqueKey).doesNotHaveDuplicates();
        assertThat(bags.findByUniqueKeys(created.stream().map(Bag::uniqueKey).toList()))
                .extracting(Bag::packageType)
                .containsOnly(PackageType.BOX);
        assertThat(lots.findLocked(LOT_A).orElseThrow().bagCount()).isEqualTo(SEEDED_BAGS_OF_LOT + NEW_BAGS);
    }
}
