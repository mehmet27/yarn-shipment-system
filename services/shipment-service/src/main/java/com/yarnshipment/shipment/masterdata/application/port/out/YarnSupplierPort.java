package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * İplik tedarikçisi okuma ve yazma portu. Liste ada göre sıralıdır. Yazmalar değişikliği yapan kullanıcıyı
 * transaction'a işler (audit); kullanılmış ad {@code DuplicateCodeException} ile reddedilir.
 */
public interface YarnSupplierPort {

    MasterDataPage<YarnSupplier> findYarnSuppliers(MasterDataQuery query);

    YarnSupplier insertYarnSupplier(UUID actorId, String name);

    /** Güncellenen kaydı döner; kayıt yoksa boş. */
    Optional<YarnSupplier> updateYarnSupplier(UUID actorId, UUID id, String name);

    /** Kayıt yoksa {@code false}. */
    boolean setYarnSupplierStatus(UUID actorId, UUID id, MasterDataStatus status);
}
