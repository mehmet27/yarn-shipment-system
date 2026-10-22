package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.model.DuplicatePlateException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import java.util.function.Supplier;
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

/** Araç ve lokasyon yazmaları gerçek PostgreSQL'de: kalıcılık, plaka tekilliği ve audit izi. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VehicleSiteCommandAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VehicleSiteCommandAdapterTest {

    private static final long AUDIT_ROWS = 3L;
    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private VehicleSiteCommandAdapter adapter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Araç eklenir, güncellenir, pasife alınır ve her adım audit_local'e aktörle düşer")
    void shouldWriteVehicleAndAuditIt() throws Exception {
        // given
        UUID actor = UUID.randomUUID();
        var owner = DATABASE.migratorJdbc();

        // when
        var created = inTx(() -> adapter.insertVehicle(actor, "35TST001"));
        boolean updated = inTx(() -> adapter.updateVehicle(actor, created.id(), "35TST002"));
        boolean off = inTx(() -> adapter.setVehicleStatus(actor, created.id(), MasterDataStatus.INACTIVE));

        // then
        assertThat(updated).isTrue();
        assertThat(off).isTrue();
        assertThat(owner.queryForObject(
                        "SELECT plate_no || '/' || active FROM md_vehicle WHERE id = ?", String.class, created.id()))
                .isEqualTo("35TST002/false");
        assertThat(owner.queryForObject(
                        "SELECT count(*) FROM audit_local WHERE table_name = 'md_vehicle' AND row_id = ?"
                                + " AND actor_id = ?",
                        Long.class,
                        created.id(),
                        actor.toString()))
                .isEqualTo(AUDIT_ROWS);
    }

    @Test
    @DisplayName("Kullanılmış plaka eklemede ve güncellemede DuplicatePlateException verir")
    void shouldRejectDuplicatePlate() throws Exception {
        // given
        UUID actor = UUID.randomUUID();
        inTx(() -> adapter.insertVehicle(actor, "35DUP001"));
        var other = inTx(() -> adapter.insertVehicle(actor, "35DUP002"));

        // when / then
        assertThatThrownBy(() -> inTx(() -> adapter.insertVehicle(actor, "35DUP001")))
                .isInstanceOf(DuplicatePlateException.class);
        assertThatThrownBy(() -> inTx(() -> adapter.updateVehicle(actor, other.id(), "35DUP001")))
                .isInstanceOf(DuplicatePlateException.class);
    }

    @Test
    @DisplayName("Lokasyon eklenir, güncellenir, pasife alınır; olmayan kayıtta false döner")
    void shouldWriteSiteAndReportMissingRows() throws Exception {
        // given
        UUID actor = UUID.randomUUID();
        var site = inTx(() -> adapter.insertSite(actor, "Yeni Depo"));

        // when
        boolean renamed = inTx(() -> adapter.updateSite(actor, site.id(), "Yeni Depo 2"));
        boolean off = inTx(() -> adapter.setSiteStatus(actor, site.id(), MasterDataStatus.INACTIVE));

        // then
        assertThat(renamed).isTrue();
        assertThat(off).isTrue();
        assertThat(DATABASE.migratorJdbc()
                        .queryForObject(
                                "SELECT name || '/' || active FROM md_site WHERE id = ?", String.class, site.id()))
                .isEqualTo("Yeni Depo 2/false");
        assertThat(inTx(() -> adapter.updateSite(actor, UUID.randomUUID(), "X")))
                .isFalse();
        assertThat(inTx(() -> adapter.setSiteStatus(actor, UUID.randomUUID(), MasterDataStatus.ACTIVE)))
                .isFalse();
        assertThat(inTx(() -> adapter.updateVehicle(actor, UUID.randomUUID(), "X")))
                .isFalse();
        assertThat(inTx(() -> adapter.setVehicleStatus(actor, UUID.randomUUID(), MasterDataStatus.ACTIVE)))
                .isFalse();
    }

    private <T> T inTx(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
}
