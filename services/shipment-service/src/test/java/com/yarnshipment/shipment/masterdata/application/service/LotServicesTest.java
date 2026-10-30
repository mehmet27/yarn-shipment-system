package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.LotEventPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LotPort;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.LotQualityLockedException;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Parti tanımı ve kalite kilidi kuralları: kapsam, kilit, bağlı kayıtların etkinliği ve olay. */
class LotServicesTest {

    private static final int PAGE_SIZE = 20;
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID LOT = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final UUID PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final UUID GRADE = UUID.fromString("0192f0a0-0000-7000-8000-000000000211");
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    private static final LotDetails DETAILS = new LotDetails("p-1", PRODUCT, GRADE, LocalDate.parse("2026-10-29"));

    private final LotPort lots = mock(LotPort.class);
    private final LotEventPort events = mock(LotEventPort.class);
    private final ScopeGuard scopeGuard = mock(ScopeGuard.class);
    private final LotManagementService management = new LotManagementService(lots, scopeGuard, CLOCK);
    private final LockLotQualityService locking = new LockLotQualityService(lots, events, scopeGuard, CLOCK);

    @Test
    @DisplayName("Kalite kullanıcısı kendi lokasyonunda parti açar; numara büyük harfe çevrilir")
    void shouldCreateLotInScope() {
        // given
        Actor quality = TestTokens.actor("quality");
        givenValidReferences();
        when(lots.activeSiteExists(TestTokens.SITE_ID)).thenReturn(true);
        when(lots.insertLot(quality.userId(), TestTokens.SITE_ID, DETAILS)).thenReturn(LOT);
        when(lots.findLot(LOT)).thenReturn(Optional.of(lot(null)));

        // when
        ProductionLot created = management.createLot(quality, TestTokens.SITE_ID, DETAILS);

        // then
        assertThat(created.details().lotNo()).isEqualTo("P-1");
        verify(scopeGuard).validateInScope(eq(quality), any(), eq("LOT_QUALITY_LOCK"));
    }

    @Test
    @DisplayName("Kapsam dışı lokasyonda parti açılamaz")
    void shouldRejectLotOutsideScope() {
        // given
        Actor quality = TestTokens.actor("quality");
        doThrow(new OutOfScopeAccessException("Site:" + OTHER_SITE))
                .when(scopeGuard)
                .validateInScope(eq(quality), any(), anyString());

        // when / then
        assertThatThrownBy(() -> management.createLot(quality, OTHER_SITE, DETAILS))
                .isInstanceOf(OutOfScopeAccessException.class);
        verify(lots, never()).insertLot(any(), any(), any());
    }

    @Test
    @DisplayName("Pasif iplik, pasif kalite ya da ileri tarihli üretim reddedilir")
    void shouldRejectInvalidReferences() {
        // given
        Actor admin = TestTokens.actor("admin");
        when(lots.activeSiteExists(TestTokens.SITE_ID)).thenReturn(true);
        when(lots.activeProductExists(PRODUCT)).thenReturn(false);
        when(lots.activeQualityGradeExists(GRADE)).thenReturn(true);
        LotDetails future = new LotDetails("P-2", PRODUCT, GRADE, LocalDate.parse("2026-10-31"));

        // when / then
        assertThatThrownBy(() -> management.createLot(admin, TestTokens.SITE_ID, DETAILS))
                .isInstanceOf(InvalidMasterDataException.class)
                .extracting(e -> ((InvalidMasterDataException) e).field())
                .isEqualTo("productId");
        when(lots.activeProductExists(PRODUCT)).thenReturn(true);
        assertThatThrownBy(() -> management.createLot(admin, TestTokens.SITE_ID, future))
                .isInstanceOf(InvalidMasterDataException.class)
                .extracting(e -> ((InvalidMasterDataException) e).field())
                .isEqualTo("productionDate");
    }

    @Test
    @DisplayName("Kilitli parti düzenlenemez")
    void shouldRejectUpdateOfLockedLot() {
        // given
        when(lots.findLot(LOT)).thenReturn(Optional.of(lot(NOW)));

        // when / then
        assertThatThrownBy(() -> management.updateLot(TestTokens.actor("admin"), LOT, DETAILS))
                .isInstanceOf(LotQualityLockedException.class);
        verify(lots, never()).updateLot(any(), any(), any());
    }

    @Test
    @DisplayName("Kilitlenen parti bu anla kilitlenir ve LotQualityLocked olayı yayınlanır")
    void shouldLockAndPublish() {
        // given
        Actor quality = TestTokens.actor("quality");
        ProductionLot locked = lot(NOW);
        when(lots.findLot(LOT)).thenReturn(Optional.of(lot(null))).thenReturn(Optional.of(locked));
        when(lots.activeQualityGradeExists(GRADE)).thenReturn(true);
        when(lots.lockQuality(quality.userId(), LOT, NOW)).thenReturn(true);

        // when
        ProductionLot result = locking.lockLotQuality(quality, LOT);

        // then
        assertThat(result.isQualityLocked()).isTrue();
        verify(events).publishQualityLocked(quality.userId(), locked);
    }

    @Test
    @DisplayName("Kilitli parti yeniden kilitlenemez, pasif parti kilitlenemez; olay yayınlanmaz")
    void shouldRejectRelockAndInactiveLot() {
        // given
        Actor admin = TestTokens.actor("admin");
        when(lots.findLot(LOT)).thenReturn(Optional.of(lot(NOW))).thenReturn(Optional.of(inactiveLot()));

        // when / then
        assertThatThrownBy(() -> locking.lockLotQuality(admin, LOT)).isInstanceOf(LotQualityLockedException.class);
        assertThatThrownBy(() -> locking.lockLotQuality(admin, LOT)).isInstanceOf(InvalidMasterDataException.class);
        verify(lots, never()).lockQuality(any(), any(), any());
        verify(events, never()).publishQualityLocked(any(), any());
    }

    @Test
    @DisplayName("Eşzamanlı kilitte koşullu güncelleme tutmazsa LOT_QUALITY_LOCKED verilir")
    void shouldReportConcurrentLock() {
        // given
        Actor admin = TestTokens.actor("admin");
        when(lots.findLot(LOT)).thenReturn(Optional.of(lot(null)));
        when(lots.activeQualityGradeExists(GRADE)).thenReturn(true);
        when(lots.lockQuality(admin.userId(), LOT, NOW)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> locking.lockLotQuality(admin, LOT)).isInstanceOf(LotQualityLockedException.class);
        verify(events, never()).publishQualityLocked(any(), any());
    }

    @Test
    @DisplayName("Olmayan parti 404 verir")
    void shouldReportMissingLot() {
        when(lots.findLot(LOT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locking.lockLotQuality(TestTokens.actor("admin"), LOT))
                .isInstanceOf(MasterDataNotFoundException.class);
    }

    @Test
    @DisplayName("Parti listesi kalite kullanıcısının kapsamıyla, ana veri yöneticisinde kapsamsız sorgulanır")
    void shouldListLotsWithScope() {
        // given
        Actor quality = TestTokens.actor("quality");
        AccessScope scope = AccessScope.restrictedTo(Set.of(TestTokens.SITE_ID), null, null);
        MasterDataQuery query = new MasterDataQuery(null, 0, PAGE_SIZE);
        when(scopeGuard.scopeOf(quality)).thenReturn(scope);

        // when
        management.listLots(quality, query);
        management.listLots(TestTokens.actor("admin"), query);

        // then
        verify(lots).findLots(scope, query);
        verify(lots).findLots(AccessScope.UNRESTRICTED, query);
    }

    private void givenValidReferences() {
        when(lots.activeProductExists(PRODUCT)).thenReturn(true);
        when(lots.activeQualityGradeExists(GRADE)).thenReturn(true);
    }

    private static ProductionLot lot(@Nullable Instant lockedAt) {
        return new ProductionLot(
                LOT,
                TestTokens.SITE_ID,
                DETAILS,
                new ProductionLot.Labels("Merkez Fabrika", "PES-30-1", "Polyester 30/1", "A", "Birinci kalite"),
                lockedAt,
                true);
    }

    private static ProductionLot inactiveLot() {
        ProductionLot lot = lot(null);
        return new ProductionLot(lot.id(), lot.siteId(), lot.details(), lot.labels(), null, false);
    }
}
