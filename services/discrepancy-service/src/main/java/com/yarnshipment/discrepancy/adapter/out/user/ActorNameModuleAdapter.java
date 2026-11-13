package com.yarnshipment.discrepancy.adapter.out.user;

import com.yarnshipment.discrepancy.application.port.out.ActorNamePort;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.user.ActorNameApi;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@link ActorNamePort}'u {@link ActorNameApi} arayüzüne bağlar. Ad çözülemezse geçmiş adsız döner; okuma
 * bozulmaz.
 */
@Component
final class ActorNameModuleAdapter implements ActorNamePort {

    private final ActorNameApi actorNameApi;

    ActorNameModuleAdapter(ActorNameApi actorNameApi) {
        this.actorNameApi = actorNameApi;
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        try {
            return actorNameApi.namesOf(userIds);
        } catch (ModuleUnavailableException exception) {
            return Map.of();
        }
    }
}
