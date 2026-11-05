package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.Bag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Çuval iptali yazma portu. */
public interface BagVoidPort {

    /**
     * Depodaki (CREATED) çuvalları iptal eder ve gerekçeyi kaydeder.
     *
     * @throws com.yarnshipment.bag.domain.model.BagNotVoidableException çuvalın durumu bu sırada değiştiyse
     */
    void voidBags(List<Bag> bags, String reason, UUID actorUserId, Instant at);
}
