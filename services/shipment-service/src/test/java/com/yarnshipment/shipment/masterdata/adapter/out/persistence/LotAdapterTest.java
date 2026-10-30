package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import java.util.UUID;
import java.util.function.Supplier;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Parti yazmaları gerçek PostgreSQL'de: tek kilit, kilitten sonra düzenleme yok, tekil numara ve outbox olayı. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LotAdapter.class, LotOutboxAdapter.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LotAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private LotAdapter lots;

    @Autowired
    private LotOutboxAdapter events;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName(
            "Parti bir kez kilitlenir; kilitten sonra düzenlenmez, numara lokasyonda tektir ve olay outbox'a girer")
    void shouldLockLotOnceAndPublishEvent() {
        // given
        LotFixtures.givenReferences(DATABASE.migratorJdbc());
        UUID lotId = inTx(() -> lots.insertLot(LotFixtures.ACTOR, LotFixtures.SITE, LotFixtures.DETAILS));

        // when
        boolean first = inTx(() -> lots.lockQuality(LotFixtures.ACTOR, lotId, LotFixtures.LOCKED_AT));
        boolean second = inTx(() -> lots.lockQuality(LotFixtures.ACTOR, lotId, LotFixtures.LOCKED_AT));
        boolean edited = inTx(() -> lots.updateLot(LotFixtures.ACTOR, lotId, LotFixtures.DETAILS));
        var locked = lots.findLot(lotId).orElseThrow();
        inTx(() -> {
            events.publishQualityLocked(LotFixtures.ACTOR, locked);
            return true;
        });

        // then
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(edited).isFalse();
        assertThat(locked.qualityLockedAt()).isEqualTo(LotFixtures.LOCKED_AT);
        assertThat(locked.labels().qualityGradeCode()).isEqualTo("LOT-A");
        assertThatThrownBy(() -> inTx(() -> lots.insertLot(LotFixtures.ACTOR, LotFixtures.SITE, LotFixtures.DETAILS)))
                .isInstanceOf(DuplicateCodeException.class);
        assertThat(DATABASE.migratorJdbc()
                        .queryForObject(
                                "SELECT payload ->> 'lotNo' || '/' || (payload ->> 'qualityGradeCode')"
                                        + " FROM outbox_event WHERE aggregate_id = ?"
                                        + " AND event_type = 'com.yarnshipment.masterdata.lot-quality-locked.v1'",
                                String.class,
                                lotId))
                .isEqualTo("T-1/LOT-A");
    }

    private <T> T inTx(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
}
