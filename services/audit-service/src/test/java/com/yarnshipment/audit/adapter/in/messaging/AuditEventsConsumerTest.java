package com.yarnshipment.audit.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import com.yarnshipment.audit.application.port.in.AppendAuditEntryUseCase;
import com.yarnshipment.common.events.IncomingEvent;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.shipment.ShipmentEvents;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditEventsConsumerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final UUID SUBJECT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000b1");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");
    private static final String TYPE = "com.yarnshipment.shipment.shipment-loaded.v1";
    private static final String DATA = "{\"status\":\"LOADED\"}";

    private final AppendAuditEntryUseCase useCase = mock(AppendAuditEntryUseCase.class);
    private final AuditEventsConsumer consumer = new AuditEventsConsumer(useCase);

    @Test
    @DisplayName("Kullanıcı olayı aktörü, rolü, korelasyonu ve gövdeyle zincire yazılır")
    void shouldAppendUserEvent() {
        consumer.consume(record(ShipmentEvents.TOPIC, ACTOR.toString()));

        AppendAuditEntryCommand command = captured();
        assertThat(command.eventId()).isEqualTo(EVENT);
        assertThat(command.occurredAt()).isEqualTo(AT);
        assertThat(command.sourceService()).isEqualTo("shipment-service");
        assertThat(command.actor().userId()).isEqualTo(ACTOR);
        assertThat(command.actor().role()).isEqualTo("WAREHOUSE");
        assertThat(command.actor().clientId()).isNull();
        assertThat(command.action()).isEqualTo(TYPE);
        assertThat(command.entityType()).isEqualTo("Shipment");
        assertThat(command.entityId()).isEqualTo(SUBJECT);
        assertThat(command.afterJson()).isEqualTo(DATA);
        assertThat(command.traceId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("Aktörsüz sistem olayı, olayı üreten servisin client kimliğiyle yazılır")
    void shouldAppendSystemEventWithClientId() {
        consumer.consume(record(DiscrepancyEvents.TOPIC, null));

        AppendAuditEntryCommand command = captured();
        assertThat(command.actor().userId()).isNull();
        assertThat(command.actor().clientId()).isEqualTo("shipment-service");
        assertThat(command.entityType()).isEqualTo("Discrepancy");
    }

    @Test
    @DisplayName("Kanıt olayı Evidence türüyle yazılır")
    void shouldMapEvidenceTopic() {
        consumer.consume(record(EvidenceEvents.TOPIC, ACTOR.toString()));

        assertThat(captured().entityType()).isEqualTo("Evidence");
    }

    private AppendAuditEntryCommand captured() {
        ArgumentCaptor<AppendAuditEntryCommand> command = ArgumentCaptor.forClass(AppendAuditEntryCommand.class);
        verify(useCase).appendEntry(command.capture());
        return command.getValue();
    }

    private static IncomingEvent record(String topic, @Nullable String actorId) {
        return new IncomingEvent(
                EVENT,
                TYPE,
                SUBJECT,
                AT,
                actorId,
                DATA,
                topic,
                "/yarn-shipment/shipment-service",
                Map.of("actorRoles", "WAREHOUSE", "correlationId", "corr-1"));
    }
}
