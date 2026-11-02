package com.yarnshipment.bag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.CreateBagsCommand;
import com.yarnshipment.bag.application.port.out.BagCreationPort;
import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.bag.domain.model.LotNotLockedException;
import com.yarnshipment.bag.domain.model.PackageType;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateBagsServiceTest {

    private static final Clock CLOCK = ProductionFixtures.CLOCK;
    private static final UUID LOT_ID = ProductionFixtures.LOT_ID;
    private static final UUID OTHER_SITE = ProductionFixtures.OTHER_SITE;

    private static final int COUNT = 3;

    private final LotQueryPort lots = mock(LotQueryPort.class);
    private final BagCreationPort creations = mock(BagCreationPort.class);
    private final CreateBagsService create =
            new CreateBagsService(lots, creations, new ScopeGuard(event -> {}, CLOCK), CLOCK);

    @Test
    @DisplayName("Kalitesi kilitli olmayan partiye çuval üretilemez")
    void shouldRejectUnlockedLot() {
        when(lots.findLocked(LOT_ID)).thenReturn(Optional.empty());
        CreateBagsCommand command =
                new CreateBagsCommand(ProductionFixtures.warehouse(), LOT_ID, PackageType.BAG, COUNT);

        assertThatThrownBy(() -> create.createBags(command)).isInstanceOf(LotNotLockedException.class);
        verify(creations, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Kapsam dışı lokasyondaki partiye çuval üretilemez")
    void shouldRejectLotOutsideScope() {
        when(lots.findLocked(LOT_ID)).thenReturn(Optional.of(ProductionFixtures.lot(OTHER_SITE)));
        CreateBagsCommand command =
                new CreateBagsCommand(ProductionFixtures.warehouse(), LOT_ID, PackageType.BAG, COUNT);

        assertThatThrownBy(() -> create.createBags(command)).isInstanceOf(OutOfScopeAccessException.class);
        verify(creations, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Çuvallar partinin kalitesiyle, işlemi yapan ve saatle üretilir")
    void shouldCreateBagsForLockedLot() {
        Actor actor = ProductionFixtures.warehouse();
        LockedLot lot = ProductionFixtures.lot(TestTokens.SITE_ID);
        List<Bag> created = List.of(ProductionFixtures.bag("BAG-AAAA-0001", BagStatus.CREATED));
        when(lots.findLocked(LOT_ID)).thenReturn(Optional.of(lot));
        when(creations.create(new BagCreationPort.NewBags(lot, PackageType.BOX, 1), actor.userId(), CLOCK.instant()))
                .thenReturn(created);

        assertThat(create.createBags(new CreateBagsCommand(actor, LOT_ID, PackageType.BOX, 1)))
                .isEqualTo(created);
    }
}
