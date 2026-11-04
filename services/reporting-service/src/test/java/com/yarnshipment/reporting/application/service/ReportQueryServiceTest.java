package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.ReportQueryPort;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Rapor kapsamı (SEC-03) ve varsayılan dönem. */
class ReportQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    private final ReportQueryPort queryPort = mock(ReportQueryPort.class);
    private final ReportQueryService service = new ReportQueryService(
            queryPort, new ReportAccess(new ScopeGuard(mock(AuditPublisher.class), CLOCK)), CLOCK);

    @Test
    @DisplayName("Yönetici kendi lokasyonu kapsamında son 30 günün özetini alır")
    void shouldQueryManagerScopeWithDefaultPeriod() {
        service.getDashboard(query(TestTokens.actor("manager")));

        ArgumentCaptor<ReportScope> scope = ArgumentCaptor.forClass(ReportScope.class);
        ArgumentCaptor<ReportPeriod> period = ArgumentCaptor.forClass(ReportPeriod.class);
        verify(queryPort).findDashboard(period.capture(), scope.capture(), any());
        assertThat(scope.getValue().siteIds()).containsExactly(TestTokens.SITE_ID);
        assertThat(scope.getValue().allSites()).isFalse();
        assertThat(period.getValue().to()).isEqualTo(LocalDate.parse("2026-11-03"));
    }

    @Test
    @DisplayName("Fasoncu ve şoför rapor görmez, sorgu çalışmaz")
    void shouldReturnEmptyForExternalRoles() {
        Dashboard dashboard = service.getDashboard(query(TestTokens.actor(TestTokens.subcontractorToken())));

        assertThat(dashboard.shipments().total()).isZero();
        assertThat(service.listDriverReport(query(TestTokens.actor(TestTokens.driverToken()))))
                .isEmpty();
        verifyNoInteractions(queryPort);
    }

    @Test
    @DisplayName("Yetkisiz lokasyon istenirse kapsam dışı hatası verir")
    void shouldRejectForeignSite() {
        ReportQuery query = query(TestTokens.actor("manager"), UUID.randomUUID());

        assertThatThrownBy(() -> service.listPairReport(query)).isInstanceOf(OutOfScopeAccessException.class);
    }

    @Test
    @DisplayName("Yönetici şoför, fasoncu ve eşleşme raporlarını kendi kapsamıyla alır")
    void shouldListPartyReportsForManager() {
        ReportQuery query = query(TestTokens.actor("manager"));

        service.listDriverReport(query);
        service.listSubcontractorReport(query);
        service.listPairReport(query);

        verify(queryPort).findDriverStats(any(), any());
        verify(queryPort).findSubcontractorStats(any(), any());
        verify(queryPort).findPairStats(any(), any());
    }

    @Test
    @DisplayName(
            "Sevkiyat dökümü ve fasoncu bakiyesi yönetici kapsamıyla, döküm limitiyle sorgulanır; dış roller boş alır")
    void shouldListShipmentAndBalanceReports() {
        service.listShipmentReport(query(TestTokens.actor("manager")));
        service.listSubcontractorBalance(query(TestTokens.actor("manager")));

        verify(queryPort).findShipments(any(), any(), eq(ReportQueryService.SHIPMENT_REPORT_LIMIT));
        verify(queryPort).findSubcontractorBalances(any(), any());
        assertThat(service.listShipmentReport(query(TestTokens.actor(TestTokens.driverToken()))))
                .isEmpty();
        assertThat(service.listSubcontractorBalance(query(TestTokens.actor(TestTokens.subcontractorToken()))))
                .isEmpty();
    }

    private static ReportQuery query(Actor actor) {
        return new ReportQuery(actor, null, null, null);
    }

    private static ReportQuery query(Actor actor, UUID siteId) {
        return new ReportQuery(actor, null, null, siteId);
    }
}
