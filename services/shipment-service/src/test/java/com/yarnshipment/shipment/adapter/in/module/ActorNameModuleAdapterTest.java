package com.yarnshipment.shipment.adapter.in.module;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.application.port.out.ActorNamePort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActorNameModuleAdapterTest {

    private static final UUID USER = UUID.randomUUID();

    @Test
    @DisplayName("Diğer modüllere sevkiyat geçmişinin kullandığı ad çözümlemesi sunulur")
    void shouldDelegateToActorNamePort() {
        ActorNamePort port = ids -> Map.of(USER, "Davut Depocu");

        assertThat(new ActorNameModuleAdapter(port).namesOf(List.of(USER))).containsEntry(USER, "Davut Depocu");
    }
}
