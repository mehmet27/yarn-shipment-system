package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.LockedLot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Kalitesi kilitli partileri okuma portu ({@code lot_quality_lock}, master-data-service'in LotQualityLocked olayı). */
public interface LotQueryPort {

    Optional<LockedLot> findLocked(UUID lotId);

    /** Verilen lokasyonlardaki partiler, en son kilitlenen önce. */
    List<LockedLot> findLockedInSites(Collection<UUID> siteIds);
}
