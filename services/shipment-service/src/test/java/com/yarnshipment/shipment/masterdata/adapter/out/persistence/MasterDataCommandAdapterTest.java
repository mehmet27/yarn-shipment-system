package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Ana veri yazmaları gerçek PostgreSQL'de: kalıcılık, silinmezlik ve audit izi. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MasterDataCommandAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MasterDataCommandAdapterTest {

    private static final long AUDIT_ROWS = 3L;
    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MasterDataCommandAdapter adapter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private <T> T inTx(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Fasoncu eklenir, güncellenir, pasife alınır ve her adım audit_local'e aktörle düşer")
    void shouldWriteSubcontractorAndAuditIt() throws Exception {
        // given
        UUID actor = UUID.randomUUID();
        var owner = DATABASE.migratorJdbc();

        // when
        var created = inTx(() -> adapter.insertSubcontractor(actor, "Yeni Tekstil"));
        boolean updated = inTx(() -> adapter.updateSubcontractor(actor, created.id(), "Yeni Tekstil AŞ"));
        boolean deactivated =
                inTx(() -> adapter.setSubcontractorStatus(actor, created.id(), MasterDataStatus.INACTIVE));

        // then
        assertThat(updated).isTrue();
        assertThat(deactivated).isTrue();
        assertThat(adapter.activeSubcontractorExists(created.id())).isFalse();
        assertThat(owner.queryForObject(
                        "SELECT title || '/' || active FROM md_subcontractor WHERE id = ?", String.class, created.id()))
                .isEqualTo("Yeni Tekstil AŞ/false");
        assertThat(owner.queryForObject(
                        "SELECT count(*) FROM audit_local WHERE table_name = 'md_subcontractor' AND row_id = ?"
                                + " AND actor_id = ?",
                        Long.class,
                        created.id(),
                        actor.toString()))
                .isEqualTo(AUDIT_ROWS);
    }

    @Test
    @DisplayName("Şoför fasoncusuyla eklenir; olmayan kayıtta güncelleme false döner")
    void shouldWriteDriverAndReportMissingRows() throws Exception {
        // given
        UUID actor = UUID.randomUUID();
        var subcontractor = inTx(() -> adapter.insertSubcontractor(actor, "Fason"));

        // when
        var driver = inTx(() -> adapter.insertDriver(actor, new DriverDetails("Ali", subcontractor.id())));
        boolean moved = inTx(() -> adapter.updateDriver(actor, driver.id(), new DriverDetails("Ali Yılmaz", null)));

        // then
        assertThat(adapter.activeSubcontractorExists(subcontractor.id())).isTrue();
        assertThat(moved).isTrue();
        assertThat(DATABASE.migratorJdbc()
                        .queryForObject("SELECT full_name FROM md_driver WHERE id = ?", String.class, driver.id()))
                .isEqualTo("Ali Yılmaz");
        assertThat(inTx(() -> adapter.updateDriver(actor, UUID.randomUUID(), new DriverDetails("X", null))))
                .isFalse();
        assertThat(inTx(() -> adapter.setDriverStatus(actor, UUID.randomUUID(), MasterDataStatus.INACTIVE)))
                .isFalse();
        assertThat(inTx(() -> adapter.updateSubcontractor(actor, UUID.randomUUID(), "X")))
                .isFalse();
    }

    @Test
    @DisplayName("Kimlik ve oluşturan sütunları değiştirilemez, kayıt silinemez")
    void shouldProtectImmutableColumns() throws Exception {
        // given
        var created = inTx(() -> adapter.insertSubcontractor(UUID.randomUUID(), "Sabit"));
        var owner = DATABASE.migratorJdbc();

        // when / then
        assertThatThrownBy(() -> owner.update(
                        "UPDATE md_subcontractor SET created_by = gen_random_uuid() WHERE id = ?", created.id()))
                .hasMessageContaining("Immutable column");
        assertThatThrownBy(() -> owner.update("DELETE FROM md_subcontractor WHERE id = ?", created.id()))
                .hasMessageContaining("cannot be deleted");
    }
}
