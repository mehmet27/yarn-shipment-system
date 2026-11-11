package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.bag.BagAssignment;
import java.time.Instant;
import java.util.UUID;

/** Çuvalları sevkiyatın çuval listesine yazar. */
public interface BagAssignmentPort {

    /** Yazılan satır sayısı; sıra numaraları sevkiyattaki son numaradan devam eder. */
    int assign(BagAssignment assignment, UUID actorUserId, Instant at);
}
