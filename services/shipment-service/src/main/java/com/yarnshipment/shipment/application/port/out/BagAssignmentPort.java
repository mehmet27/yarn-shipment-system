package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.bag.BagAssignment;
import java.time.Instant;
import java.util.UUID;

/** Ayrılan çuvalları sevkiyatın çuval listesine yazar. */
public interface BagAssignmentPort {

    /** Yeni yazılan satır sayısı; sevkiyatta zaten olan çuvallar atlanır. */
    int assign(BagAssignment assignment, UUID actorUserId, Instant at);
}
