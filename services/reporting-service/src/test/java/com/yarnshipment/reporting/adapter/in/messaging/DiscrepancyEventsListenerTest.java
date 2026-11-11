package com.yarnshipment.reporting.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeUseCase;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** {@code discrepancy.events.v1} olaylarının okuma modeli komutuna çevrilmesi. */
class DiscrepancyEventsListenerTest {

    private static final String PREFIX = "com.yarnshipment.discrepancy.";
    private static final String HEAD = "\"discrepancyId\":\"" + UUID.randomUUID() + "\",\"discrepancyNo\":\"UY-1\","
            + "\"shipmentId\":\"" + EventRecords.SHIPMENT + "\",\"siteId\":\"" + EventRecords.SITE + "\"";

    private final ApplyReadModelChangeUseCase useCase = mock(ApplyReadModelChangeUseCase.class);
    private final DiscrepancyEventsListener listener = new DiscrepancyEventsListener(useCase);

    @Test
    @DisplayName("Açılış olayı vade ve öncelik taşır, açılış zamanı olay zamanıdır")
    void shouldMapOpened() {
        String json = "{" + HEAD + ",\"type\":\"QUALITY\",\"severity\":\"RED\",\"ruleCode\":\"QTY\","
                + "\"dueAt\":\"2026-11-10T08:00:00Z\"}";

        var change = (ReadModelChange.DiscrepancyOpened) apply("discrepancy-opened.v1", json);

        assertThat(change.severity()).isEqualTo("RED");
        assertThat(change.openedAt()).hasToString(EventRecords.AT);
    }

    @Test
    @DisplayName("Çözüm olayı kalan açık sayısını taşır")
    void shouldMapResolved() {
        String json = "{" + HEAD + ",\"resolution\":\"TOLERANCE\",\"resolvedAt\":\"" + EventRecords.AT
                + "\",\"remainingOpen\":1}";

        var change = (ReadModelChange.DiscrepancyResolved) apply("discrepancy-resolved.v1", json);

        assertThat(change.remainingOpen()).isEqualTo(1);
    }

    @Test
    @DisplayName("Kullanılmayan olay türü onaylanır ve okuma modeline gitmez")
    void shouldIgnoreUnusedEvent() {
        listener.onDiscrepancyEvent(EventRecords.record(DiscrepancyEvents.TOPIC, PREFIX + "other.v1", "{}"));

        verifyNoInteractions(useCase);
    }

    private ReadModelChange apply(String type, String json) {
        listener.onDiscrepancyEvent(EventRecords.record(DiscrepancyEvents.TOPIC, PREFIX + type, json));
        ArgumentCaptor<ApplyReadModelChangeCommand> command =
                ArgumentCaptor.forClass(ApplyReadModelChangeCommand.class);
        verify(useCase).applyChange(command.capture());
        return command.getValue().change();
    }
}
