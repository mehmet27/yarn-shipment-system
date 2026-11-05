package com.yarnshipment.bag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.VoidBagsCommand;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.BagVoidPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagNotVoidableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VoidBagsServiceTest {

    private static final Clock CLOCK = ProductionFixtures.CLOCK;
    private static final UUID OTHER_SITE = ProductionFixtures.OTHER_SITE;

    private final BagQueryPort bags = mock(BagQueryPort.class);
    private final BagVoidPort voids = mock(BagVoidPort.class);
    private final VoidBagsService service = new VoidBagsService(bags, voids, new ScopeGuard(event -> {}, CLOCK), CLOCK);

    @Test
    @DisplayName("Depodaki çuvallar gerekçeyle iptal edilir; zaten iptal edilmiş olan sayılmaz")
    void shouldVoidWarehouseBagsAndSkipVoided() {
        Actor actor = ProductionFixtures.warehouse();
        Bag fresh = ProductionFixtures.bag("BAG-AAAA-0001", BagStatus.CREATED);
        Bag voided = ProductionFixtures.bag("BAG-AAAA-0002", BagStatus.VOID);
        when(bags.findByUniqueKeys(any())).thenReturn(List.of(voided, fresh));

        int count = service.voidBags(
                new VoidBagsCommand(actor, List.of(fresh.uniqueKey(), voided.uniqueKey()), " Yanlış üretildi "));

        assertThat(count).isEqualTo(1);
        verify(voids).voidBags(List.of(fresh), "Yanlış üretildi", actor.userId(), CLOCK.instant());
    }

    @Test
    @DisplayName("Sevkiyata ayrılmış ya da yola çıkmış çuval iptal edilemez ve hiçbiri iptal edilmez")
    void shouldRejectBagsOutsideWarehouse() {
        Actor actor = ProductionFixtures.warehouse();
        Bag fresh = ProductionFixtures.bag("BAG-AAAA-0001", BagStatus.CREATED);
        for (BagStatus status : List.of(BagStatus.RESERVED, BagStatus.SHIPPED, BagStatus.DELIVERED)) {
            Bag other = ProductionFixtures.bag("BAG-AAAA-0002", status);
            when(bags.findByUniqueKeys(any())).thenReturn(List.of(fresh, other));
            VoidBagsCommand command =
                    new VoidBagsCommand(actor, List.of(fresh.uniqueKey(), other.uniqueKey()), "Neden");
            assertThatThrownBy(() -> service.voidBags(command)).isInstanceOf(BagNotVoidableException.class);
        }
        verify(voids, never()).voidBags(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Bulunamayan anahtar 404, kapsam dışı lokasyon yasak olur")
    void shouldRejectMissingAndForeignBags() {
        Actor actor = ProductionFixtures.warehouse();
        when(bags.findByUniqueKeys(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.voidBags(new VoidBagsCommand(actor, List.of("YOK"), "Neden")))
                .isInstanceOf(BagNotFoundException.class);

        Bag foreign = ProductionFixtures.bagAt(OTHER_SITE, "BAG-AAAA-0005", BagStatus.CREATED);
        when(bags.findByUniqueKeys(any())).thenReturn(List.of(foreign));
        VoidBagsCommand command = new VoidBagsCommand(actor, List.of(foreign.uniqueKey()), "Neden");
        assertThatThrownBy(() -> service.voidBags(command)).isInstanceOf(OutOfScopeAccessException.class);
        verify(voids, never()).voidBags(any(), any(), any(), any());
    }
}
