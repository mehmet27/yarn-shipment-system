package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.UUID;

/**
 * Araç ve lokasyon yazma portu. Her yazma değişikliği yapan kullanıcıyı transaction'a işler (audit); bulunamayan
 * kayıt için güncelleme ve durum değişikliği {@code false} döner. Kullanılmış plaka {@code DuplicatePlateException}
 * ile reddedilir.
 */
public interface VehicleSiteCommandPort {

    Vehicle insertVehicle(UUID actorId, String plateNo);

    boolean updateVehicle(UUID actorId, UUID id, String plateNo);

    boolean setVehicleStatus(UUID actorId, UUID id, MasterDataStatus status);

    Site insertSite(UUID actorId, String name);

    boolean updateSite(UUID actorId, UUID id, String name);

    boolean setSiteStatus(UUID actorId, UUID id, MasterDataStatus status);
}
