package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.catalog.PackagingType;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandard;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandardDetails;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Standart dara okuma ve yazma portu. Yazma değişikliği yapan kullanıcıyı transaction'a işler (audit); olmayan
 * lokasyon {@code InvalidMasterDataException} ile reddedilir.
 */
public interface TareStandardPort {

    List<TareStandard> findTareStandards(@Nullable UUID siteId);

    /** Lokasyon ve ambalaj tipi için kaydı ekler, varsa değerlerini günceller. */
    TareStandard upsertTareStandard(
            UUID actorId, UUID siteId, PackagingType packagingType, TareStandardDetails details);
}
