package com.yarnshipment.discrepancy.adapter.out.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.user.ActorNameApi;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActorNameModuleAdapterTest {

    private static final UUID USER = UUID.randomUUID();

    @Test
    @DisplayName("Adlar user modülünden olduğu gibi döner")
    void shouldReturnNames() {
        ActorNameApi api = ids -> Map.of(USER, "Davut Depocu");

        assertThat(new ActorNameModuleAdapter(api).namesOf(List.of(USER))).containsEntry(USER, "Davut Depocu");
    }

    @Test
    @DisplayName("Modül yanıt vermezse adsız döner, okuma bozulmaz")
    void shouldReturnEmptyWhenUnavailable() {
        ActorNameApi api = ids -> {
            throw new ModuleUnavailableException("down");
        };

        assertThat(new ActorNameModuleAdapter(api).namesOf(List.of(USER))).isEmpty();
    }
}
