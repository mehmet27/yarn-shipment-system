package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Parti okuma ve yazma portu. Yazmalar değişikliği yapan kullanıcıyı transaction'a işler (audit). Lokasyonda
 * kullanılmış parti numarası {@code DuplicateCodeException} ile reddedilir. Kilit kuralı veritabanında da zorlanır.
 */
public interface LotPort {

    Optional<ProductionLot> findLot(UUID id);

    /** Kapsam filtresi WHERE koşuluna çevrilir; arama parti numarası, iplik kodu ve adında yapılır. */
    MasterDataPage<ProductionLot> findLots(AccessScope scope, MasterDataQuery query);

    UUID insertLot(UUID actorId, UUID siteId, LotDetails details);

    /** Yalnızca kilitsiz partiyi günceller; parti bu arada kilitlendiyse {@code false}. */
    boolean updateLot(UUID actorId, UUID id, LotDetails details);

    boolean setLotStatus(UUID actorId, UUID id, MasterDataStatus status);

    /** Kilitsiz partiyi kilitler; parti zaten kilitliyse {@code false}. */
    boolean lockQuality(UUID actorId, UUID id, Instant lockedAt);

    boolean activeSiteExists(UUID id);

    boolean activeProductExists(UUID id);

    boolean activeQualityGradeExists(UUID id);
}
