package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.AssignDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscrepancyDecisionServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");

    private final DiscrepancyQueryPort queries = mock(DiscrepancyQueryPort.class);
    private final DiscrepancyDecisionPort decisions = mock(DiscrepancyDecisionPort.class);
    private final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, CLOCK);
    private final AssignDiscrepancyService assign = new AssignDiscrepancyService(queries, decisions, scopeGuard, CLOCK);
    private final ResolveDiscrepancyService resolve =
            new ResolveDiscrepancyService(queries, decisions, scopeGuard, CLOCK);

    @Test
    @DisplayName("Yetkili olunmayan lokasyondaki uyuşmazlık kapsam dışıdır ve karar yazılmaz")
    void shouldRejectOutOfScopeSite() {
        when(queries.findDiscrepancy(TestDiscrepancies.ID)).thenReturn(Optional.of(TestDiscrepancies.open(OTHER_SITE)));
        ResolveDiscrepancyCommand command =
                new ResolveDiscrepancyCommand(quality(), TestDiscrepancies.ID, Resolution.TOLERANCE, "uygun", null);

        assertThatThrownBy(() -> resolve.resolveDiscrepancy(command)).isInstanceOf(OutOfScopeAccessException.class);
        verify(decisions, never()).saveResolution(any(), any(), any());
    }

    @Test
    @DisplayName("Atama incelemeye alır ve yazımı yapan kişi ve zamanla kaydeder")
    void shouldAssign() {
        Actor actor = quality();
        UUID assignee = UUID.randomUUID();
        when(queries.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)));

        Discrepancy assigned =
                assign.assignDiscrepancy(new AssignDiscrepancyCommand(actor, TestDiscrepancies.ID, assignee));

        assertThat(assigned.status()).isEqualTo(DiscrepancyStatus.IN_REVIEW);
        verify(decisions).saveAssignment(assigned, actor.userId(), NOW);
    }

    @Test
    @DisplayName("Karar notu kırpılır ve karar veren ile zaman kayda yazılır")
    void shouldResolve() {
        Actor actor = quality();
        when(queries.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)));

        Discrepancy resolved = resolve.resolveDiscrepancy(new ResolveDiscrepancyCommand(
                actor, TestDiscrepancies.ID, Resolution.DATA_ERROR, "  tartı hatası  ", null));

        assertThat(resolved.status()).isEqualTo(DiscrepancyStatus.RESOLVED);
        Discrepancy.Decision decision = Objects.requireNonNull(resolved.decision());
        assertThat(decision.note()).isEqualTo("tartı hatası");
        assertThat(decision.resolvedBy()).isEqualTo(actor.userId());
        verify(decisions).saveResolution(resolved, actor.userId(), NOW);
    }

    private static Actor quality() {
        return TestTokens.actor("quality");
    }
}
