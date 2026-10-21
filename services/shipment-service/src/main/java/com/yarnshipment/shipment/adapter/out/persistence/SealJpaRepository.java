package com.yarnshipment.shipment.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface SealJpaRepository extends Repository<SealJpaEntity, UUID> {

    /** Sevkiyatların geçerli (düzeltilmemiş) mühür kayıtları. */
    List<SealJpaEntity> findByShipmentIdInAndSupersededByIsNull(Collection<UUID> shipmentIds);
}
