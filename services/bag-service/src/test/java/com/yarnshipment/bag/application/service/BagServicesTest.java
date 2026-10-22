package com.yarnshipment.bag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.ReleaseBagsCommand;
import com.yarnshipment.bag.application.port.in.ReserveBagsCommand;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.BagReservationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.PackageType;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BagServicesTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000009a1");
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);

    private final BagQueryPort queries = mock(BagQueryPort.class);
    private final BagReservationPort reservations = mock(BagReservationPort.class);
    private final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, CLOCK);
    private final ReserveBagsService reserve = new ReserveBagsService(queries, reservations, scopeGuard, CLOCK);
    private final ReleaseBagsService release = new ReleaseBagsService(queries, reservations, scopeGuard, CLOCK);

    @Test
    @DisplayName("Bulunamayan anahtar BAG_NOT_FOUND verir ve hiçbir çuval ayrılmaz")
    void shouldFailWhenKeyIsMissing() throws Exception {
        Actor actor = TestTokens.actor("warehouse");
        when(queries.findByUniqueKeys(any())).thenReturn(List.of(bag("BAG-0001", TestTokens.SITE_ID)));
        ReserveBagsCommand command = new ReserveBagsCommand(actor, SHIPMENT, List.of("BAG-0001", "YOK"));

        assertThatThrownBy(() -> reserve.reserveBags(command)).isInstanceOf(BagNotFoundException.class);
        verify(reservations, never()).reserve(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Kapsam dışı lokasyondaki çuval ayrılamaz")
    void shouldRejectBagOutsideScope() throws Exception {
        Actor actor = TestTokens.actor("warehouse");
        when(queries.findByUniqueKeys(any())).thenReturn(List.of(bag("BAG-0005", OTHER_SITE)));
        ReserveBagsCommand command = new ReserveBagsCommand(actor, SHIPMENT, List.of("BAG-0005"));

        assertThatThrownBy(() -> reserve.reserveBags(command)).isInstanceOf(RuntimeException.class);
        verify(reservations, never()).reserve(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Kapsamdaki çuvallar ayrılır ve güncel hâlleriyle döner")
    void shouldReserveBagsInScope() throws Exception {
        Actor actor = TestTokens.actor("warehouse");
        List<Bag> bags = List.of(bag("BAG-0001", TestTokens.SITE_ID));
        when(queries.findByUniqueKeys(any())).thenReturn(bags);

        List<Bag> result = reserve.reserveBags(new ReserveBagsCommand(actor, SHIPMENT, List.of("BAG-0001")));

        assertThat(result).isEqualTo(bags);
        verify(reservations).reserve(SHIPMENT, bags, actor.userId(), CLOCK.instant());
    }

    @Test
    @DisplayName("Bırakma kapsamdaki rezervasyonları bırakır ve sayıyı döner")
    void shouldReleaseReservations() throws Exception {
        Actor actor = TestTokens.actor("admin");
        when(queries.findReservedFor(SHIPMENT)).thenReturn(List.of(bag("BAG-0001", TestTokens.SITE_ID)));
        when(reservations.release(SHIPMENT, "iptal", actor.userId(), CLOCK.instant()))
                .thenReturn(1);

        assertThat(release.releaseBags(new ReleaseBagsCommand(actor, SHIPMENT, "iptal")))
                .isEqualTo(1);
    }

    private static Bag bag(String key, UUID siteId) {
        return new Bag(UUID.randomUUID(), siteId, key, PackageType.BAG, "P-2026-0001", "A", BagStatus.CREATED, null);
    }
}
