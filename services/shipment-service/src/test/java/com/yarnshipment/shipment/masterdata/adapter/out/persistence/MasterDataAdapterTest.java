package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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

/** Ana veri sorguları gerçek PostgreSQL'de, yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MasterDataAdapter.class)
class MasterDataAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID SITE_A = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final int PAGE_SIZE = 20;
    private static final int SAMPLE_DRIVERS = 3;
    private static final int SAMPLE_SITES = 2;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MasterDataAdapter adapter;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Şoförler ada göre sıralı gelir ve fasoncuya göre süzülür")
    void shouldListDriversOrderedAndFilteredBySubcontractor() {
        var all = adapter.findDrivers(query(null), null);
        UUID subcontractor = all.items().stream()
                .map(Driver::subcontractorId)
                .filter(id -> id != null)
                .findFirst()
                .orElseThrow();
        var filtered = adapter.findDrivers(query(null), subcontractor);

        assertThat(all.totalElements()).isEqualTo(SAMPLE_DRIVERS);
        assertThat(all.items()).extracting(Driver::fullName).isSorted();
        assertThat(all.items())
                .filteredOn(driver -> driver.subcontractorId() != null)
                .isNotEmpty()
                .allMatch(driver -> driver.subcontractorTitle() != null);
        assertThat(filtered.items()).isNotEmpty().allMatch(driver -> subcontractor.equals(driver.subcontractorId()));
    }

    @Test
    @DisplayName("Arama büyük-küçük harfe duyarsızdır ve LIKE joker karakterleri düz metin sayılır")
    void shouldSearchCaseInsensitivelyAndEscapeWildcards() {
        assertThat(adapter.findVehicles(query("34abc")).items()).hasSize(1);
        assertThat(adapter.findVehicles(query("%")).items()).isEmpty();
        assertThat(adapter.findVehicles(query("_")).items()).isEmpty();
    }

    @Test
    @DisplayName("Pasif kayıt listede görünmez")
    void shouldHideInactiveRows() {
        JdbcTemplate owner = DATABASE.migratorJdbc();
        owner.update("UPDATE md_vehicle SET active = false WHERE plate_no = '34ABC123'");
        try {
            assertThat(adapter.findVehicles(query("34ABC")).items()).isEmpty();
            var withInactive = adapter.findVehicles(new MasterDataQuery("34ABC", 0, PAGE_SIZE, true));
            assertThat(withInactive.items()).hasSize(1);
            assertThat(withInactive.items().getFirst().active()).isFalse();
        } finally {
            owner.update("UPDATE md_vehicle SET active = true WHERE plate_no = '34ABC123'");
        }
    }

    @Test
    @DisplayName("Lokasyonlar kapsama göre süzülür; kapsamsız kullanıcı boş sayfa alır")
    void shouldFilterSitesByScope() {
        var scoped = adapter.findSites(new AccessScope(Set.of(SITE_A), null, null, false), query(null));
        var unrestricted = adapter.findSites(AccessScope.UNRESTRICTED, query(null));
        var none = adapter.findSites(new AccessScope(Set.of(), null, null, false), query(null));

        assertThat(scoped.items()).extracting(Site::id).containsExactly(SITE_A);
        assertThat(unrestricted.totalElements()).isEqualTo(SAMPLE_SITES);
        assertThat(none.items()).isEmpty();
        assertThat(none.totalElements()).isZero();
    }

    @Test
    @DisplayName("Sayfalama toplam sayıyı korur")
    void shouldPaginate() {
        var second = adapter.findSubcontractors(new MasterDataQuery(null, 1, 2));

        assertThat(second.items()).hasSize(1);
        assertThat(second.totalElements()).isEqualTo(SAMPLE_DRIVERS);
        assertThat(second.totalPages()).isEqualTo(2);
    }

    private static MasterDataQuery query(@Nullable String text) {
        return new MasterDataQuery(text, 0, PAGE_SIZE);
    }
}
