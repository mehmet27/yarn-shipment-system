package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Hesap açma hedeflerinin okunması gerçek PostgreSQL'de, yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LoginAccountTargetAdapter.class)
class LoginAccountTargetAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");

    @Autowired
    private LoginAccountTargetAdapter adapter;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Lokasyonlardan yalnızca etkin ve var olanlar döner")
    void shouldFindOnlyActiveSites() {
        UUID unknown = UUID.randomUUID();

        assertThat(adapter.findActiveSiteIds(List.of(SITE, unknown))).isEqualTo(Set.of(SITE));
        assertThat(adapter.findActiveSiteIds(List.of())).isEmpty();
    }
}
