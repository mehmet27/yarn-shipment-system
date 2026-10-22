package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import java.util.UUID;

/**
 * Ana veri yazma portu. Her yazma, değişikliği yapan kullanıcıyı transaction'a işler (audit); bulunamayan kayıt
 * için güncelleme ve durum değişikliği {@code false} döner.
 */
public interface MasterDataCommandPort {

    Subcontractor insertSubcontractor(UUID actorId, String title);

    boolean updateSubcontractor(UUID actorId, UUID id, String title);

    boolean setSubcontractorStatus(UUID actorId, UUID id, MasterDataStatus status);

    boolean activeSubcontractorExists(UUID id);

    Driver insertDriver(UUID actorId, DriverDetails details);

    boolean updateDriver(UUID actorId, UUID id, DriverDetails details);

    boolean setDriverStatus(UUID actorId, UUID id, MasterDataStatus status);
}
