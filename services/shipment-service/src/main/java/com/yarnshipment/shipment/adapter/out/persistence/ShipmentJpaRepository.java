package com.yarnshipment.shipment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

interface ShipmentJpaRepository
        extends Repository<ShipmentJpaEntity, UUID>, JpaSpecificationExecutor<ShipmentJpaEntity> {

    Optional<ShipmentJpaEntity> findById(UUID id);
}
