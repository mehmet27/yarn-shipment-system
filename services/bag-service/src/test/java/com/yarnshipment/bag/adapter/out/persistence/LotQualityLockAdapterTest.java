package com.yarnshipment.bag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.bag.domain.model.LotQualityLock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Parti kalite kilidinin yerel kopyası: tekrar yazım etkisiz, çuval yalnızca kilitli partinin kalitesini alır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LotQualityLockAdapter.class)
class LotQualityLockAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-000000000202");
    private static final Instant LOCKED_AT = Instant.parse("2026-10-30T10:00:00Z");
    private static final String INSERT_BAG = """
            INSERT INTO bag (id, site_id, unique_key, package_type, lot_id, lot_no, product_id, quality_grade_code,
                             status, created_at, created_by)
            VALUES (?, ?, ?, 'BAG', ?, 'P-9', ?, ?, 'CREATED', now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private LotQualityLockAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Kilit bir kez yazılır; aynı parti ikinci kez gelirse değişiklik olmaz")
    void shouldInsertOnce() {
        UUID lot = UUID.randomUUID();
        LotQualityLock lock = new LotQualityLock(lot, SITE, "P-9", PRODUCT, "A", LOCKED_AT);

        assertThat(adapter.insertIfAbsent(lock, LOCKED_AT)).isTrue();
        assertThat(adapter.insertIfAbsent(lock, LOCKED_AT)).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT quality_grade_code FROM lot_quality_lock WHERE lot_id = ?", String.class, lot))
                .isEqualTo("A");
    }

    @Test
    @DisplayName("Çuval kilitli partiye kilitteki kaliteyle bağlanır; başka kalite veya kilitsiz parti reddedilir")
    void shouldBindBagsOnlyToLockedQuality() {
        UUID lot = UUID.randomUUID();
        adapter.insertIfAbsent(new LotQualityLock(lot, SITE, "P-9", PRODUCT, "A", LOCKED_AT), LOCKED_AT);

        jdbc.update(INSERT_BAG, UUID.randomUUID(), SITE, "LQ-1", lot, PRODUCT, "A", SITE);

        assertThatThrownBy(() -> jdbc.update(INSERT_BAG, UUID.randomUUID(), SITE, "LQ-2", lot, PRODUCT, "B", SITE))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
                        jdbc.update(INSERT_BAG, UUID.randomUUID(), SITE, "LQ-3", UUID.randomUUID(), PRODUCT, "A", SITE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
