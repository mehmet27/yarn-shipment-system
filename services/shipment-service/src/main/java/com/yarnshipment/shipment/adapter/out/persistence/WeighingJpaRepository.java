package com.yarnshipment.shipment.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface WeighingJpaRepository extends Repository<WeighingJpaEntity, UUID> {

    /** Sevkiyatların geçerli (düzeltilmemiş) tartımları. */
    List<WeighingJpaEntity> findByShipmentIdInAndSupersededByIsNull(Collection<UUID> shipmentIds);
}
