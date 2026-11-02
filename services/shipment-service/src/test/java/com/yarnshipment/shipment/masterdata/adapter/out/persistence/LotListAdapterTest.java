package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import java.util.Set;
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

/** Parti listesi gerçek PostgreSQL'de: arama ve lokasyon kapsamı WHERE koşuluna çevrilir. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LotAdapter.class)
class LotListAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final int PAGE_SIZE = 20;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private LotAdapter lots;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Liste kapsamdaki lokasyonların partilerini getirir")
    void shouldFilterLotsByScope() {
        // given
        var owner = DATABASE.migratorJdbc();
        LotFixtures.givenReferences(owner);
        owner.update(
                "INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id,"
                        + " production_date, created_at, created_by)"
                        + " VALUES (?, ?, 'SCOPE-1', ?, ?, current_date, now(), ?) ON CONFLICT DO NOTHING",
                UUID.fromString("0192f0a0-0000-7000-8000-0000000a1004"),
                LotFixtures.SITE,
                LotFixtures.PRODUCT,
                LotFixtures.GRADE,
                LotFixtures.ACTOR);
        MasterDataQuery query = new MasterDataQuery("scope", 0, PAGE_SIZE);

        // when / then
        assertThat(lots.findLots(AccessScope.restrictedTo(Set.of(LotFixtures.SITE), null, null), query)
                        .items())
                .extracting(lot -> lot.details().lotNo())
                .containsExactly("SCOPE-1");
        assertThat(lots.findLots(AccessScope.restrictedTo(Set.of(UUID.randomUUID()), null, null), query)
                        .items())
                .isEmpty();
        assertThat(lots.findLots(AccessScope.UNRESTRICTED, query).totalElements())
                .isEqualTo(1L);
    }
}
