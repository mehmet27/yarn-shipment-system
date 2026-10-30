package com.yarnshipment.bag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.ListLockedLotsQuery;
import com.yarnshipment.bag.application.port.in.ListLotBagsQuery;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LabeledBag;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListLotsServicesTest {

    private static final Clock CLOCK = ProductionFixtures.CLOCK;
    private static final UUID LOT_ID = ProductionFixtures.LOT_ID;
    private static final UUID OTHER_SITE = ProductionFixtures.OTHER_SITE;

    private final LotQueryPort lots = mock(LotQueryPort.class);
    private final BagQueryPort bags = mock(BagQueryPort.class);
    private final ListLockedLotsService listLots = new ListLockedLotsService(lots);
    private final ListLotBagsService listBags =
            new ListLotBagsService(lots, bags, new ScopeGuard(event -> {}, CLOCK));

    @Test
    @DisplayName("Parti listesi isteyenin lokasyonlarıyla sorgulanır")
    void shouldListLotsOfActorSites() {
        Actor actor = ProductionFixtures.warehouse();
        when(lots.findLockedInSites(actor.siteIds())).thenReturn(List.of(ProductionFixtures.lot(TestTokens.SITE_ID)));

        assertThat(listLots.listLockedLots(new ListLockedLotsQuery(actor))).hasSize(1);
    }

    @Test
    @DisplayName("Partinin çuvalları yalnızca kapsamdaki kilitli parti için listelenir")
    void shouldListBagsOfLotInScope() {
        Actor actor = ProductionFixtures.warehouse();
        List<LabeledBag> labeled =
                List.of(new LabeledBag(ProductionFixtures.bag("BAG-AAAA-0001", BagStatus.CREATED), 0, null));
        when(lots.findLocked(LOT_ID)).thenReturn(Optional.of(ProductionFixtures.lot(TestTokens.SITE_ID)));
        when(bags.findByLot(eq(LOT_ID), anyInt())).thenReturn(labeled);

        assertThat(listBags.listLotBags(new ListLotBagsQuery(actor, LOT_ID))).isEqualTo(labeled);

        when(lots.findLocked(LOT_ID)).thenReturn(Optional.of(ProductionFixtures.lot(OTHER_SITE)));
        assertThatThrownBy(() -> listBags.listLotBags(new ListLotBagsQuery(actor, LOT_ID)))
                .isInstanceOf(OutOfScopeAccessException.class);
    }
}
