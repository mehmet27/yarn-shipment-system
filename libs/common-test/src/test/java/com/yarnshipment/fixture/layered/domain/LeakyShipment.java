package com.yarnshipment.fixture.layered.domain;

import com.yarnshipment.fixture.layered.adapter.out.persistence.ShipmentJpaEntity;

/** Domain'in kalıcılık modeline bağımlı olduğu kasıtlı ihlal. */
public record LeakyShipment(ShipmentJpaEntity entity) {}
