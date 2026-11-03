package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.outbox.ProcessedEvents;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** İdempotent tüketici kaydı ortak sınıfa devredilir. */
class ProcessedEventAdapterTest {

    @Test
    @DisplayName("Olayın ilk işlenişi true, tekrarı false döner")
    void shouldDelegateToProcessedEvents() {
        ProcessedEvents processedEvents = mock(ProcessedEvents.class);
        UUID id = UUID.randomUUID();
        when(processedEvents.markProcessed(id, "t")).thenReturn(true, false);
        ProcessedEventAdapter adapter = new ProcessedEventAdapter(processedEvents);

        assertThat(adapter.markProcessed(id, "t")).isTrue();
        assertThat(adapter.markProcessed(id, "t")).isFalse();
    }
}
