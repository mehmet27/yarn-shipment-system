package com.yarnshipment.fixture.layered.adapter.in.web;

import com.yarnshipment.fixture.layered.adapter.out.persistence.ShipmentJpaEntity;

/** Controller'ın persistence adapter'ını doğrudan gördüğü kasıtlı ihlal. */
public final class LeakyController {

    public String getShipment(ShipmentJpaEntity entity) {
        return entity.shipmentNo();
    }
}
