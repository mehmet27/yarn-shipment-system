package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotFoundException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscrepancyQueryServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");
    private static final int PAGE_SIZE = 20;
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ListDiscrepanciesQuery.Filter NO_FILTER =
            new ListDiscrepanciesQuery.Filter(null, null, null, null);

    private final DiscrepancyQueryPort queries = mock(DiscrepancyQueryPort.class);
    private final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, CLOCK);
    private final GetDiscrepancyService get = new GetDiscrepancyService(queries, scopeGuard);
    private final ListDiscrepanciesService list = new ListDiscrepanciesService(queries, scopeGuard);

    @Test
    @DisplayName("Olmayan uyuşmazlık bulunamadı hatası verir")
    void shouldFailWhenMissing() {
        when(queries.findDiscrepancy(TestDiscrepancies.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> get.getDiscrepancy(new GetDiscrepancyQuery(quality(), TestDiscrepancies.ID)))
                .isInstanceOf(DiscrepancyNotFoundException.class);
    }

    @Test
    @DisplayName("Fasoncu kendi sevkiyatının uyuşmazlığını görür; başka fasoncunun ya da fasoncusu boş kaydı görmez")
    void shouldScopeSubcontractorToOwnDiscrepancies() {
        Actor subcontractor = TestTokens.actor(TestTokens.subcontractorToken());
        when(queries.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)))
                .thenReturn(Optional.of(
                        TestDiscrepancies.of(TestTokens.SITE_ID, UUID.randomUUID(), DiscrepancyStatus.OPEN)))
                .thenReturn(Optional.of(TestDiscrepancies.of(TestTokens.SITE_ID, null, DiscrepancyStatus.OPEN)));
        GetDiscrepancyQuery query = new GetDiscrepancyQuery(subcontractor, TestDiscrepancies.ID);

        assertThat(get.getDiscrepancy(query).subcontractorId()).isEqualTo(TestTokens.SUBCONTRACTOR_ID);
        assertThatThrownBy(() -> get.getDiscrepancy(query)).isInstanceOf(OutOfScopeAccessException.class);
        assertThatThrownBy(() -> get.getDiscrepancy(query)).isInstanceOf(OutOfScopeAccessException.class);
    }

    @Test
    @DisplayName("Fasoncunun listesi kendi fasoncu kimliğiyle süzülür")
    void shouldListSubcontractorDiscrepanciesByOwner() {
        Actor subcontractor = TestTokens.actor(TestTokens.subcontractorToken());
        when(queries.findDiscrepancies(any(), any(), eq(0), eq(PAGE_SIZE)))
                .thenReturn(DiscrepancyPage.empty(0, PAGE_SIZE));

        list.listDiscrepancies(new ListDiscrepanciesQuery(subcontractor, NO_FILTER, 0, PAGE_SIZE));

        verify(queries)
                .findDiscrepancies(
                        any(),
                        argThat(scope -> TestTokens.SUBCONTRACTOR_ID.equals(scope.subcontractorId())),
                        eq(0),
                        eq(PAGE_SIZE));
    }

    @Test
    @DisplayName("Şoför uyuşmazlık göremez; liste sorgusu hiç çalışmaz")
    void shouldHideFromDriver() {
        when(queries.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)));
        Actor driver = TestTokens.actor(TestTokens.driverToken());

        assertThatThrownBy(() -> get.getDiscrepancy(new GetDiscrepancyQuery(driver, TestDiscrepancies.ID)))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThat(list.listDiscrepancies(new ListDiscrepanciesQuery(driver, NO_FILTER, 0, PAGE_SIZE))
                        .items())
                .isEmpty();
        verify(queries, never()).findDiscrepancies(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Liste, kullanıcının lokasyon kapsamıyla sorgulanır")
    void shouldListWithinScope() {
        DiscrepancyPage page =
                new DiscrepancyPage(List.of(TestDiscrepancies.open(TestTokens.SITE_ID)), 0, PAGE_SIZE, 1);
        when(queries.findDiscrepancies(any(), any(), eq(0), eq(PAGE_SIZE))).thenReturn(page);

        DiscrepancyPage result = list.listDiscrepancies(new ListDiscrepanciesQuery(quality(), NO_FILTER, 0, PAGE_SIZE));

        assertThat(result.totalElements()).isEqualTo(1);
        verify(queries)
                .findDiscrepancies(
                        any(), argThat(scope -> scope.siteIds().contains(TestTokens.SITE_ID)), eq(0), eq(PAGE_SIZE));
    }

    private static Actor quality() {
        return TestTokens.actor("quality");
    }
}
