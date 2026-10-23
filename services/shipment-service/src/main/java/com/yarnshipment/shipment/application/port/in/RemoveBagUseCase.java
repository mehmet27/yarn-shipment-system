package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Çuvalı taslak sevkiyattan çıkarır, çuval servisinde rezervasyonunu bırakır; çuval listede değilse sessizce geçer. */
public interface RemoveBagUseCase {

    void removeBag(RemoveBagCommand command);

    /** Çuvalı taslak sevkiyattan çıkarma isteği. */
    record RemoveBagCommand(Actor actor, UUID shipmentId, String uniqueKey) {}
}
