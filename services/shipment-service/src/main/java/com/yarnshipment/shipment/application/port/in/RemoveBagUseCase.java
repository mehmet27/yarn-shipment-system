package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Çuvalı sıra numarasıyla taslak sevkiyattan çıkarır; çuval listede değilse sessizce geçer. */
public interface RemoveBagUseCase {

    void removeBag(RemoveBagCommand command);

    /** Çuvalı taslak sevkiyattan çıkarma isteği. */
    record RemoveBagCommand(Actor actor, UUID shipmentId, int seqNo) {}
}
