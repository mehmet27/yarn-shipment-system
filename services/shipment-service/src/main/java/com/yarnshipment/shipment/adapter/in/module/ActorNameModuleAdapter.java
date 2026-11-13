package com.yarnshipment.shipment.adapter.in.module;

import com.yarnshipment.moduleapi.user.ActorNameApi;
import com.yarnshipment.shipment.application.port.out.ActorNamePort;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Diğer modüllerin {@link ActorNameApi} çağrısını, sevkiyat geçmişinin kullandığı ad çözümlemesiyle karşılar. */
@Component
final class ActorNameModuleAdapter implements ActorNameApi {

    private final ActorNamePort actorNames;

    ActorNameModuleAdapter(ActorNamePort actorNames) {
        this.actorNames = actorNames;
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        return actorNames.namesOf(userIds);
    }
}
