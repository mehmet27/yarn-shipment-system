package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.out.ActorNamePort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListDiscrepancyActivityServiceTest {

    private static final UUID DISCREPANCY = TestDiscrepancies.ID;
    private static final UUID QUALITY = UUID.randomUUID();
    private static final UUID MANAGER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");

    private final DiscrepancyQueryPort queryPort = mock(DiscrepancyQueryPort.class);
    private final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Geçmiş kayıtlarına işlemi yapanın ve atananın adı eklenir")
    void shouldAttachActorAndAssigneeNames() {
        Instant at = Instant.parse("2026-10-20T11:00:00Z");
        List<DiscrepancyActivity> stored = List.of(
                new DiscrepancyActivity(UUID.randomUUID(), "OPENED", "QUALITY_CLAIM", null, QUALITY, at),
                new DiscrepancyActivity(UUID.randomUUID(), "ASSIGNED", MANAGER.toString(), null, QUALITY, at));
        stubFindActivity(stored);
        ActorNamePort names = ids -> Map.of(QUALITY, "Kerem Kalite", MANAGER, "Melek Müdür");

        List<DiscrepancyActivity> result = service(names).listActivity(query());

        assertThat(result.get(0).actorName()).isEqualTo("Kerem Kalite");
        assertThat(result.get(0).assigneeName()).isNull();
        assertThat(result.get(1).assigneeName()).isEqualTo("Melek Müdür");
    }

    @Test
    @DisplayName("Ad çözülemezse kayıtlar adsız döner, okuma bozulmaz")
    void shouldReturnActivityWithoutNamesWhenUnresolved() {
        Instant at = Instant.parse("2026-10-20T11:00:00Z");
        stubFindActivity(List.of(
                new DiscrepancyActivity(UUID.randomUUID(), "ASSIGNED", "bozuk-not", null, QUALITY, at)));
        ActorNamePort names = (Collection<UUID> ids) -> Map.of();

        List<DiscrepancyActivity> result = service(names).listActivity(query());

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.actorName()).isNull();
            assertThat(item.assigneeName()).isNull();
        });
    }

    private void stubFindActivity(List<DiscrepancyActivity> stored) {
        when(queryPort.findActivity(DISCREPANCY)).thenReturn(stored);
        when(queryPort.findDiscrepancy(DISCREPANCY))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)));
    }

    private ListDiscrepancyActivityService service(ActorNamePort names) {
        return new ListDiscrepancyActivityService(queryPort, scopeGuard, names);
    }

    private static GetDiscrepancyQuery query() {
        return new GetDiscrepancyQuery(TestTokens.actor("quality"), DISCREPANCY);
    }
}
