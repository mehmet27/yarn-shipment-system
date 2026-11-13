package com.yarnshipment.common.security.session;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

/** Oturumları defter üzerinden "süresi doldu" işaretler; tek uygulama kopyası için yeterlidir. */
public final class RegistrySessionTerminator implements SessionTerminator {

    private final SessionRegistry registry;

    public RegistrySessionTerminator(SessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void terminateAll(UUID userId) {
        terminate(userId, null);
    }

    @Override
    public void terminateOthers(UUID userId, String keepSessionId) {
        terminate(userId, keepSessionId);
    }

    private void terminate(UUID userId, @Nullable String keep) {
        for (Object principal : registry.getAllPrincipals()) {
            if (principal instanceof Actor actor && actor.userId().equals(userId)) {
                for (SessionInformation session : registry.getAllSessions(principal, false)) {
                    if (keep == null || !session.getSessionId().equals(keep)) {
                        session.expireNow();
                    }
                }
            }
        }
    }
}
