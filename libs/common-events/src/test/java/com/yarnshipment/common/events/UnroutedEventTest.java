package com.yarnshipment.common.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.yarnshipment.moduleapi.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

class UnroutedEventTest {

    private static final String TYPE = "com.yarnshipment.other.other-done.v1";

    @Test
    @DisplayName("Yönlendirmesi olmayan olay türü sessizce yutulmaz, yayın hata verir")
    void shouldRejectEventWithoutRoute() {
        InProcessDomainEventPublisher publisher = new InProcessDomainEventPublisher(
                JsonMapper.builder().build(),
                List::of,
                mock(PlatformTransactionManager.class),
                new EventRoutes(List.of()));
        DomainEvent unrouted = new DomainEvent("Other", UUID.randomUUID(), TYPE, "x", Instant.EPOCH, UUID.randomUUID());

        assertThatThrownBy(() -> publisher.publish(unrouted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TYPE);
    }
}
