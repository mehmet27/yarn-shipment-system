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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Hesap açma hedeflerinin okunması gerçek PostgreSQL'de, yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LoginAccountTargetAdapter.class)
class LoginAccountTargetAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final UUID DRIVER = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private LoginAccountTargetAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Etkin şoför ve fasoncu bulunur; pasife alınan ya da olmayan kayıt bulunmaz")
    void shouldFindOnlyActiveRecords() {
        assertThat(adapter.findActiveDriverName(DRIVER)).isPresent();
        assertThat(adapter.findActiveSubcontractorTitle(SUBCONTRACTOR)).isPresent();
        assertThat(adapter.findActiveDriverName(UUID.randomUUID())).isEmpty();

        jdbc.update("UPDATE md_driver SET active = false WHERE id = ?", DRIVER);

        assertThat(adapter.findActiveDriverName(DRIVER)).isEmpty();
    }

    @Test
    @DisplayName("Lokasyonlardan yalnızca etkin ve var olanlar döner")
    void shouldFindOnlyActiveSites() {
        UUID unknown = UUID.randomUUID();

        assertThat(adapter.findActiveSiteIds(List.of(SITE, unknown))).isEqualTo(Set.of(SITE));
        assertThat(adapter.findActiveSiteIds(List.of())).isEmpty();
    }
}
