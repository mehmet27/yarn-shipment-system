package com.yarnshipment.bag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.PrintLabelsCommand;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.LabelPrintPort;
import com.yarnshipment.bag.application.port.out.LabelRenderer;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagLabel;
import com.yarnshipment.bag.domain.model.BagNotAvailableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LabelReprintReasonRequiredException;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrintLabelsServiceTest {

    private static final Clock CLOCK = ProductionFixtures.CLOCK;
    private static final UUID OTHER_SITE = ProductionFixtures.OTHER_SITE;

    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final int EARLIER_PRINTS = 2;

    private final BagQueryPort bags = mock(BagQueryPort.class);
    private final LabelPrintPort prints = mock(LabelPrintPort.class);
    private final LabelRenderer renderer = mock(LabelRenderer.class);
    private final PrintLabelsService print =
            new PrintLabelsService(bags, prints, renderer, new ScopeGuard(event -> {}, CLOCK), CLOCK);

    @Test
    @DisplayName("İlk baskı gerekçesiz yapılır, istenen sırayla basılır ve kaydedilir")
    void shouldPrintFirstLabelsInRequestOrder() {
        Actor actor = ProductionFixtures.warehouse();
        Bag first = ProductionFixtures.bag("BAG-ZZZZ-0001", BagStatus.CREATED);
        Bag second = ProductionFixtures.bag("BAG-AAAA-0002", BagStatus.RESERVED);
        when(bags.findByUniqueKeys(any())).thenReturn(List.of(second, first));
        when(prints.lockAndCountPrints(anyList())).thenReturn(Map.of());
        List<BagLabel> expected = List.of(new BagLabel(first, 1), new BagLabel(second, 1));
        when(renderer.render(expected)).thenReturn(PDF);

        byte[] pdf = print.printLabels(
                new PrintLabelsCommand(actor, List.of(first.uniqueKey(), second.uniqueKey()), "  "));

        assertThat(pdf).isEqualTo(PDF);
        verify(prints).record(expected, null, actor.userId(), CLOCK.instant());
    }

    @Test
    @DisplayName("Daha önce basılmış etiket gerekçesiz yeniden basılamaz ve hiçbir baskı kaydedilmez")
    void shouldRequireReasonForReprint() {
        Bag printed = ProductionFixtures.bag("BAG-AAAA-0001", BagStatus.CREATED);
        when(bags.findByUniqueKeys(any())).thenReturn(List.of(printed));
        when(prints.lockAndCountPrints(anyList())).thenReturn(Map.of(printed.id(), 1));
        PrintLabelsCommand command =
                new PrintLabelsCommand(ProductionFixtures.warehouse(), List.of(printed.uniqueKey()), null);

        assertThatThrownBy(() -> print.printLabels(command)).isInstanceOf(LabelReprintReasonRequiredException.class);
        verify(prints, never()).record(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Gerekçeli yeniden baskı bir sonraki baskı numarasını alır")
    void shouldReprintWithReason() {
        Actor actor = ProductionFixtures.warehouse();
        Bag printed = ProductionFixtures.bag("BAG-AAAA-0001", BagStatus.CREATED);
        when(bags.findByUniqueKeys(any())).thenReturn(List.of(printed));
        when(prints.lockAndCountPrints(anyList())).thenReturn(Map.of(printed.id(), EARLIER_PRINTS));
        when(renderer.render(any())).thenReturn(PDF);

        print.printLabels(new PrintLabelsCommand(actor, List.of(printed.uniqueKey()), " etiket yırtıldı "));

        List<BagLabel> expected = List.of(new BagLabel(printed, EARLIER_PRINTS + 1));
        verify(prints).record(expected, "etiket yırtıldı", actor.userId(), CLOCK.instant());
    }

    @Test
    @DisplayName("Bulunamayan anahtar, iptal edilmiş ya da yola çıkmış çuval için etiket basılmaz")
    void shouldRejectMissingOrUnprintableBags() {
        Actor actor = ProductionFixtures.warehouse();
        when(bags.findByUniqueKeys(any())).thenReturn(List.of());
        assertThatThrownBy(() -> print.printLabels(new PrintLabelsCommand(actor, List.of("YOK"), null)))
                .isInstanceOf(BagNotFoundException.class);

        for (BagStatus status : List.of(BagStatus.VOID, BagStatus.SHIPPED, BagStatus.DELIVERED)) {
            Bag bag = ProductionFixtures.bag("BAG-AAAA-0001", status);
            when(bags.findByUniqueKeys(any())).thenReturn(List.of(bag));
            PrintLabelsCommand command = new PrintLabelsCommand(actor, List.of(bag.uniqueKey()), null);
            assertThatThrownBy(() -> print.printLabels(command)).isInstanceOf(BagNotAvailableException.class);
        }
        verify(prints, never()).record(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Kapsam dışı lokasyondaki çuvalın etiketi basılamaz")
    void shouldRejectLabelOutsideScope() {
        Bag foreign = ProductionFixtures.bagAt(OTHER_SITE, "BAG-AAAA-0005", BagStatus.CREATED);
        when(bags.findByUniqueKeys(any())).thenReturn(List.of(foreign));
        PrintLabelsCommand command =
                new PrintLabelsCommand(ProductionFixtures.warehouse(), List.of(foreign.uniqueKey()), null);

        assertThatThrownBy(() -> print.printLabels(command)).isInstanceOf(OutOfScopeAccessException.class);
    }
}
